package com.passthepaper.service;

import com.passthepaper.dto.ResourceDto;
import com.passthepaper.entity.*;
import com.passthepaper.exception.AppException;
import com.passthepaper.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepo;
    private final UserRepository userRepo;
    private final LogService logService;

    public List<ResourceDto.Response> getFeatured() {
        return resourceRepo.findFeatured(PageRequest.of(0, 6))
                .stream().map(ResourceDto.Response::from).collect(Collectors.toList());
    }

    public List<ResourceDto.Response> browse(String category, String search, int page, int size) {
        return resourceRepo.search(category, search, PageRequest.of(page, size))
                .stream().map(ResourceDto.Response::from).collect(Collectors.toList());
    }

    public ResourceDto.Response getById(UUID id) {
        return resourceRepo.findById(id)
                .map(ResourceDto.Response::from)
                .orElseThrow(() -> new AppException("Resource not found"));
    }

    @Transactional
    public ResourceDto.Response upload(UUID userId, ResourceDto.CreateRequest req, String fileUrl) {
        User uploader = userRepo.findById(userId)
                .orElseThrow(() -> new AppException("User not found"));

        if (!Boolean.TRUE.equals(uploader.getIsVerified())) {
            throw new AppException("Only verified users can upload resources");
        }
        if (!Boolean.TRUE.equals(uploader.getCanUpload())) {
            throw new AppException("Upload permission revoked");
        }

        Resource.PriceType priceType;
        try { priceType = Resource.PriceType.valueOf(req.priceType()); }
        catch (Exception e) { throw new AppException("Invalid priceType"); }

        Resource resource = Resource.builder()
                .title(req.title())
                .description(req.description())
                .category(req.category())
                .price(req.price())
                .priceType(priceType)
                .uploadedBy(uploader)
                .uploaderName(uploader.getName())
                .status(Resource.ResourceStatus.pending)
                .fileUrl(fileUrl)
                .department(req.department())
                .course(req.course())
                .semester(req.semester())
                .build();

        resourceRepo.save(resource);
        logService.log(Log.LogType.user_action, "UPLOAD", "User uploaded resource: " + req.title(),
                userId, uploader.getName(), null, null, null);
        return ResourceDto.Response.from(resource);
    }

    @Transactional
    public void approveResource(UUID resourceId, boolean approve, UUID adminId) {
        Resource r = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new AppException("Resource not found"));
        r.setStatus(approve ? Resource.ResourceStatus.approved : Resource.ResourceStatus.rejected);
        resourceRepo.save(r);

        // Reward uploader on approval
        if (approve) {
            User uploader = r.getUploadedBy();
            uploader.setRewardPoints(uploader.getRewardPoints() + 100);
            userRepo.save(uploader);
        }
        logService.log(Log.LogType.admin_action,
                approve ? "RESOURCE_APPROVED" : "RESOURCE_REJECTED",
                (approve ? "Approved" : "Rejected") + " resource: " + r.getTitle(),
                adminId, null, r.getUploadedBy().getId(), r.getUploadedBy().getName(),
                Map.of("resourceId", resourceId.toString()));
    }

    @Transactional
    public void deleteResource(UUID resourceId, UUID adminId) {
        Resource r = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new AppException("Resource not found"));
        resourceRepo.delete(r);
        logService.log(Log.LogType.admin_action, "RESOURCE_DELETED",
                "Deleted resource: " + r.getTitle(), adminId, null, null, null, null);
    }

    public List<ResourceDto.Response> getMyUploads(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new AppException("User not found"));
        return resourceRepo.findByUploadedByOrderByCreatedAtDesc(user)
                .stream().map(ResourceDto.Response::from).collect(Collectors.toList());
    }

    public List<ResourceDto.Response> getPendingResources() {
        return resourceRepo.findByStatusOrderByCreatedAtDesc(Resource.ResourceStatus.pending)
                .stream().map(ResourceDto.Response::from).collect(Collectors.toList());
    }
}
