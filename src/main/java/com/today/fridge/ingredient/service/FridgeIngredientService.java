package com.today.fridge.ingredient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.today.fridge.file.dto.FileAssetDto;
import com.today.fridge.file.service.FileAssetService;
import com.today.fridge.global.exception.BusinessException;
import com.today.fridge.global.exception.ErrorCode;
import com.today.fridge.global.external.fastapi.EstimateExpirationResponse;
import com.today.fridge.global.external.fastapi.FastApiService;
import com.today.fridge.global.response.PageResponse;
import com.today.fridge.ingredient.domain.FreshnessCalculator;
import com.today.fridge.ingredient.domain.StorageTypePolicy;
import com.today.fridge.ingredient.dto.CategoryResponse;
import com.today.fridge.ingredient.dto.CreateIngredientRequest;
import com.today.fridge.ingredient.dto.DeleteIngredientData;
import com.today.fridge.ingredient.dto.FridgeIngredientListData;
import com.today.fridge.ingredient.dto.FridgeSummaryResponse;
import com.today.fridge.ingredient.dto.IngredientResponse;
import com.today.fridge.ingredient.dto.RemoteImageStagingRequest;
import com.today.fridge.ingredient.dto.RemoteImageStagingResponse;
import com.today.fridge.ingredient.dto.RemoteImageStagingResultRequest;
import com.today.fridge.ingredient.dto.RemoteImageUploadResultRequest;
import com.today.fridge.ingredient.dto.SoonItemResponse;
import com.today.fridge.global.upload.BytesMultipartFile;
import com.today.fridge.ingredient.dto.vision.VisionRecognizeDataDto;
import com.today.fridge.vision.dto.VisionRecognitionStatusDto;
import com.today.fridge.vision.service.VisionRecognitionQueryService;
import com.today.fridge.ingredient.entity.IngredientCategory;
import com.today.fridge.file.entity.FileAsset;
import com.today.fridge.ingredient.entity.IngredientMaster;
import com.today.fridge.ingredient.entity.UserIngredient;
import com.today.fridge.ingredient.repository.IngredientCategoryRepository;
import com.today.fridge.ingredient.repository.IngredientMasterRepository;
import com.today.fridge.ingredient.repository.UserIngredientRepository;
import com.today.fridge.ingredient.type.FreshnessStatus;
import com.today.fridge.ingredient.type.StorageType;
import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@Transactional(readOnly = true)
public class FridgeIngredientService {

    private static final int NAME_MAX_LEN = 100;
    private static final int UNIT_MAX_LEN = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final FastApiService fastApiService;
    private final VisionRecognitionQueryService visionRecognitionQueryService;
    private final FileAssetService fileAssetService;
    private final ObjectMapper objectMapper;

    public FridgeIngredientService(
            UserIngredientRepository userIngredientRepository,
            UserRepository userRepository,
            IngredientMasterRepository ingredientMasterRepository,
            IngredientCategoryRepository ingredientCategoryRepository,
            FastApiService fastApiService,
            VisionRecognitionQueryService visionRecognitionQueryService,
            FileAssetService fileAssetService,
            ObjectMapper objectMapper) {
        this.userIngredientRepository = userIngredientRepository;
        this.userRepository = userRepository;
        this.ingredientMasterRepository = ingredientMasterRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.fastApiService = fastApiService;
        this.visionRecognitionQueryService = visionRecognitionQueryService;
        this.fileAssetService = fileAssetService;
        this.objectMapper = objectMapper;
    }

    public List<CategoryResponse> listCategories() {
        return ingredientCategoryRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(c -> new CategoryResponse(c.getCategoryId(), c.getCategoryCode(), c.getCategoryName(), c.getSortOrder()))
                .toList();
    }

    public IngredientResponse getOne(Long userId, Long ingredientId) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        return toResponse(e);
    }

    public FridgeIngredientListData list(
            Long userId,
            int page,
            int size,
            String sort,
            String freshnessStatus,
            String storageType,
            String keyword,
            Long categoryId) {
        FreshnessStatus freshness = parseFreshnessFilter(freshnessStatus);
        if (StringUtils.hasText(storageType)) {
            StorageTypePolicy.validateOrThrow(storageType);
        }
        if (categoryId != null) {
            validateCategoryId(categoryId);
        }
        LocalDate today = LocalDate.now(KST);
        LocalDate soonEnd = today.plusDays(FreshnessCalculator.SOON_DAYS_INCLUSIVE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<UserIngredient> result = userIngredientRepository.searchFridgePage(
                userId, today, soonEnd, freshness, storageType, keyword, categoryId, sort, pageable);
        List<IngredientResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        PageResponse pageInfo = new PageResponse(
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize());
        return new FridgeIngredientListData(items, pageInfo);
    }

    public FridgeSummaryResponse summary(Long userId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate soonEnd = today.plusDays(FreshnessCalculator.SOON_DAYS_INCLUSIVE);
        long total = userIngredientRepository.countByUser_UserId(userId);
        long expired = userIngredientRepository.countByUser_UserIdAndExpiresAtBefore(userId, today);
        long soon = userIngredientRepository.countByUser_UserIdAndExpiresAtSoonWindow(userId, today, soonEnd);
        long fresh = total - expired - soon;
        if (fresh < 0) {
            fresh = 0;
        }
        Page<UserIngredient> soonPage =
                userIngredientRepository.findSoonPageForSummary(
                        userId,
                        today,
                        soonEnd,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "expiresAt")));
        List<SoonItemResponse> soonItems =
                soonPage.getContent().stream()
                        .map(
                                ui ->
                                        new SoonItemResponse(
                                                ui.getUserIngredientId(),
                                                ui.getRawName(),
                                                ui.getExpiresAt(),
                                                FreshnessCalculator.computeStatus(ui.getExpiresAt(), today)
                                                        .name()))
                        .toList();
        return new FridgeSummaryResponse(total, fresh, soon, expired, soonItems);
    }

    @Transactional
    public IngredientResponse create(Long userId, CreateIngredientRequest req) {
        validatePositiveQuantity(req.getQuantity());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String storage = StringUtils.hasText(req.getStorageType()) ? req.getStorageType() : StorageType.REFRIGERATED.name();
        StorageTypePolicy.validateOrThrow(storage);

        UserIngredient e = new UserIngredient();
        e.setUser(user);
        e.setRawName(req.getName().trim());
        e.setNormalizedNameSnapshot(req.getName().trim());
        e.setQuantity(req.getQuantity());
        validateUnitLength(req.getUnit());
        e.setUnit(req.getUnit());
        e.setStorageType(storage);

        tryAttachMaster(e);

        Long catId = null;
        if (req.getCategoryId() != null) {
            Long validated = validateCategoryId(req.getCategoryId());
            // 미분류(UNKNOWN)는 "카테고리 미선택"과 동일 취급 → 마스터·휴리스틱 적용
            if (!isUnknownCategoryId(validated)) {
                catId = validated;
            }
        }
        if (catId == null) {
            Long fromMaster = categoryIdFromLinkedMaster(e.getIngredientMaster());
            Long heuristic = inferCategoryFromNameHeuristic(e.getRawName());
            if (fromMaster != null && isUnknownCategoryId(fromMaster) && heuristic != null) {
                catId = heuristic;
            } else if (fromMaster != null) {
                catId = fromMaster;
            } else {
                catId = heuristic;
            }
        }
        e.setCategoryId(catId);

        if (Boolean.TRUE.equals(req.getRemoteImagePending())) {
            if (req.getFileId() != null) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR, "remote_image_pending 과 file_id 는 함께 사용할 수 없습니다.");
            }
            if (!StringUtils.hasText(req.getRemoteImageMimeType())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "remote_image_mime_type 이 필요합니다.");
            }
            FileAsset pending =
                    fileAssetService.createApachePendingVision(
                            user,
                            req.getRemoteImageOriginalName(),
                            req.getRemoteImageMimeType(),
                            req.getRemoteImageSize());
            e.setFileAsset(pending);
        } else if (req.getFileId() != null) {
            e.setFileAsset(fileAssetService.getOwnedFileOrThrow(req.getFileId(), userId));
        } else if (req.getImageFile() != null) {
            var savedAsset = fileAssetService.saveFromUploadMetadata(user, req.getImageFile());
            e.setFileAsset(savedAsset);
        }

        if (req.getExpirationDate() != null) {
            e.setExpiresAt(req.getExpirationDate());
        } else {
            // 유통기한 미입력 → FastAPI 규칙 기반 추정
            String catCode = resolveCategoryCode(catId);
            EstimateExpirationResponse est = fastApiService.estimateExpiration(req.getName(), catCode, storage);
            if (est != null) {
                e.setExpiresAt(est.estimatedExpirationDate());
            } else {
                e.setExpiresAt(FastApiService.fallbackExpiration(storage));
            }
        }

        UserIngredient saved = userIngredientRepository.save(e);
        return toResponse(saved);
    }

    @Transactional
    public IngredientResponse applyApacheImageSync(
            Long userId, Long ingredientId, RemoteImageUploadResultRequest req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        UserIngredient ui =
                userIngredientRepository
                        .findByIdAndUserId(ingredientId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        FileAsset fa = ui.getFileAsset();
        if (fa == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "연결된 이미지가 없습니다.");
        }
        if (!Boolean.TRUE.equals(req.getSuccess())) {
            Long fid = fa.getFileId();
            ui.setFileAsset(null);
            userIngredientRepository.save(ui);
            fileAssetService.deleteByIdAndUser(fid, userId);
            UserIngredient reloaded =
                    userIngredientRepository
                            .findByIdAndUserId(ingredientId, userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
            return toResponse(reloaded);
        }
        fileAssetService.applyApacheUploadMetadata(
                fa.getFileId(), userId, req.getSha1sum(), req.getFileSize(), req.getMimeType());
        UserIngredient reloaded =
                userIngredientRepository
                        .findByIdAndUserId(ingredientId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        return toResponse(reloaded);
    }

    @Transactional
    public RemoteImageStagingResponse stageApacheImageReplaceStaging(
            Long userId, Long ingredientId, RemoteImageStagingRequest req) {
        userIngredientRepository
                .findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        if (req == null || !StringUtils.hasText(req.getMimeType())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "mime_type 이 필요합니다.");
        }
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        FileAsset pending =
                fileAssetService.createApachePendingVision(
                        user, req.getOriginalName(), req.getMimeType(), req.getFileSize());
        return new RemoteImageStagingResponse(pending.getFileId());
    }

    @Transactional
    public IngredientResponse applyApacheImageReplaceStagingResult(
            Long userId, Long ingredientId, RemoteImageStagingResultRequest req) {
        if (req == null || req.getPendingFileId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "pending_file_id 가 필요합니다.");
        }
        UserIngredient ui =
                userIngredientRepository
                        .findByIdAndUserId(ingredientId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        FileAsset pending = fileAssetService.getOwnedFileOrThrow(req.getPendingFileId(), userId);
        if (!Boolean.TRUE.equals(req.getSuccess())) {
            fileAssetService.deleteByIdAndUser(req.getPendingFileId(), userId);
            return toResponse(ui);
        }
        FileAsset old = ui.getFileAsset();
        ui.setFileAsset(pending);
        userIngredientRepository.save(ui);
        if (old != null && !old.getFileId().equals(pending.getFileId())) {
            fileAssetService.deleteByIdAndUser(old.getFileId(), userId);
        }
        fileAssetService.applyApacheUploadMetadata(
                pending.getFileId(), userId, req.getSha1sum(), req.getFileSize(), req.getMimeType());
        UserIngredient reloaded =
                userIngredientRepository
                        .findByIdAndUserId(ingredientId, userId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        return toResponse(reloaded);
    }

    /**
     * §4.4 — {@link com.today.fridge.ingredient.dto.UpdateIngredientRequest}와 동일 키.
     * {@code Map}으로 받아 전달된 키만 반영한다.
     */
    @Transactional
    public IngredientResponse patch(Long userId, Long ingredientId, Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));

        if (body.containsKey("name")) {
            Object v = body.get("name");
            if (v == null || (v instanceof String s && s.isBlank())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            String n = v.toString().trim();
            validateNameLength(n);
            e.setRawName(n);
            e.setNormalizedNameSnapshot(n);
        }
        if (body.containsKey("quantity")) {
            Object v = body.get("quantity");
            if (v == null) {
                e.setQuantity(null);
            } else {
                BigDecimal q = toBigDecimal(v);
                validatePositiveQuantity(q);
                e.setQuantity(q);
            }
        }
        if (body.containsKey("unit")) {
            Object v = body.get("unit");
            if (v == null) {
                e.setUnit(null);
            } else {
                String u = v.toString();
                if (u.length() > UNIT_MAX_LEN) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                e.setUnit(u);
            }
        }
        if (body.containsKey("storageType")) {
            Object v = body.get("storageType");
            if (v == null) {
                e.setStorageType(null);
            } else {
                String st = v.toString();
                StorageTypePolicy.validateOrThrow(st);
                e.setStorageType(st);
            }
        }
        if (body.containsKey("expirationDate")) {
            Object v = body.get("expirationDate");
            if (v == null) {
                e.setExpiresAt(null);
            } else {
                e.setExpiresAt(parseLocalDate(v));
            }
        }
        if (body.containsKey("categoryId")) {
            Object v = body.get("categoryId");
            if (v == null) {
                e.setCategoryId(null);
            } else {
                Long rawCat = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString());
                Long validated = validateCategoryId(rawCat);
                e.setCategoryId(isUnknownCategoryId(validated) ? null : validated);
            }
        }
        maybeApplyImageFilePatch(userId, e, body);
        maybeApplyFileIdPatch(userId, e, body);

        tryAttachMaster(e);
        tryFillCategoryFromMasterWhenUnset(e, body.containsKey("categoryId"));
        UserIngredient saved = userIngredientRepository.save(e);
        return toResponse(saved);
    }

    @Transactional
    public DeleteIngredientData delete(Long userId, Long ingredientId) {
        UserIngredient e = userIngredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
        long id = e.getUserIngredientId();
        FileAsset fa = e.getFileAsset();
        if (fa != null) {
            e.setFileAsset(null);
            userIngredientRepository.save(e);
            userIngredientRepository.flush();
        }
        userIngredientRepository.delete(e);
        if (fa != null) {
            fileAssetService.deleteByIdAndUser(fa.getFileId(), userId);
        }
        return new DeleteIngredientData(id);
    }

    private static FreshnessStatus parseFreshnessFilter(String freshnessStatus) {
        if (!StringUtils.hasText(freshnessStatus)) {
            return null;
        }
        try {
            return FreshnessStatus.valueOf(freshnessStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void tryAttachMaster(UserIngredient e) {
        e.setIngredientMaster(null);
        String norm = StringUtils.hasText(e.getNormalizedNameSnapshot())
                ? e.getNormalizedNameSnapshot().trim()
                : (e.getRawName() != null ? e.getRawName().trim() : "");
        if (!StringUtils.hasText(norm)) {
            return;
        }
        resolveMaster(norm).ifPresent(e::setIngredientMaster);
    }

    /**
     * 표준명 일치 → {@code ingredient_master} 연결.
     * 없으면 {@code alias_text}(예: {@code ko:당근,…}) 부분 일치로 후보 검색 (시스템설계서: 정규화·카테고리 분류 보조).
     */
    private Optional<IngredientMaster> resolveMaster(String rawOrNormalized) {
        String q = sanitizeForMasterLookup(rawOrNormalized);
        if (!StringUtils.hasText(q)) {
            return Optional.empty();
        }
        Optional<IngredientMaster> exact = ingredientMasterRepository.findByCanonicalNameIgnoreCase(q);
        if (exact.isPresent()) {
            return exact;
        }
        List<IngredientMaster> candidates = ingredientMasterRepository.findCandidatesByCanonicalNameOrAlias(
                q, PageRequest.of(0, 5));
        return candidates.stream().findFirst();
    }

    private static String sanitizeForMasterLookup(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("%", "").replace("_", "").trim();
        if (s.length() > 100) {
            return s.substring(0, 100);
        }
        return s;
    }

    /** {@code ingredient_master.category_id}를 {@code ingredient_category} PK로 변환 (존재할 때만). */
    private Long categoryIdFromLinkedMaster(IngredientMaster master) {
        if (master == null || master.getCategoryId() == null) {
            return null;
        }
        long cid = master.getCategoryId().longValue();
        return ingredientCategoryRepository.existsById(cid) ? cid : null;
    }

    /**
     * PATCH에서 {@code categoryId} 키가 없고 현재 값이 비어 있으면 마스터 기준 자동 분류.
     */
    private void tryFillCategoryFromMasterWhenUnset(UserIngredient e, boolean categoryFieldPresentInPatch) {
        if (categoryFieldPresentInPatch) {
            return;
        }
        if (e.getCategoryId() != null) {
            return;
        }
        Long fromMaster = categoryIdFromLinkedMaster(e.getIngredientMaster());
        Long heuristic = inferCategoryFromNameHeuristic(e.getRawName());
        if (fromMaster != null && isUnknownCategoryId(fromMaster) && heuristic != null) {
            e.setCategoryId(heuristic);
            return;
        }
        if (fromMaster != null) {
            e.setCategoryId(fromMaster);
            return;
        }
        if (heuristic != null) {
            e.setCategoryId(heuristic);
        }
    }

    /** API·프론트 URL은 항상 {@code vision/{file_id}.확장자} 형태로 맞춘다 (DB에 UUID 파일명이 남아 있어도). */
    private static String fileAssetStoredExtension(FileAsset fa) {
        String stored = fa.getStoredName();
        if (stored != null) {
            int dot = stored.lastIndexOf('.');
            if (dot >= 0 && dot < stored.length() - 1) {
                String ext = stored.substring(dot).toLowerCase();
                if (ext.length() <= 10) {
                    return ext;
                }
            }
        }
        String mime = fa.getMimeType();
        if (mime == null) {
            return ".jpg";
        }
        return switch (mime.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    private IngredientResponse toResponse(UserIngredient ui) {
        LocalDate today = LocalDate.now(KST);
        FreshnessStatus status = FreshnessCalculator.computeStatus(ui.getExpiresAt(), today);
        String imgPath = null;
        String imgStored = null;
        Long imgFileId = null;
        if (ui.getFileAsset() != null) {
            FileAsset fa = ui.getFileAsset();
            imgFileId = fa.getFileId();
            String ext = fileAssetStoredExtension(fa);
            imgStored = imgFileId + ext;
            imgPath = "vision/" + imgStored;
        }
        return new IngredientResponse(
                ui.getUserIngredientId(),
                ui.getRawName(),
                ui.getNormalizedNameSnapshot(),
                ui.getCategoryId(),
                resolveCategoryName(ui),
                ui.getExpiresAt(),
                ui.getQuantity(),
                ui.getUnit(),
                ui.getStorageType(),
                status.name(),
                imgPath,
                imgStored,
                imgFileId);
    }

    /**
     * PATCH 본문의 {@code file_id}: 숫자면 소유 파일 연결, JSON {@code null}이면 이미지 해제.
     * 인식 API 직후 같은 업로드 분을 식재료에 매칭한다 ({@code maybeApplyImageFilePatch} 다음 줄에서 실행된다 —
     * 두 필드를 함께 보내면 {@code file_id}가 최종 반영).
     */
    private void maybeApplyFileIdPatch(Long userId, UserIngredient e, Map<String, Object> body) {
        if (!body.containsKey("file_id")) {
            return;
        }
        Object raw = body.get("file_id");
        if (raw == null) {
            e.setFileAsset(null);
            return;
        }
        long fid = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString());
        FileAsset fa = fileAssetService.getOwnedFileOrThrow(fid, userId);
        e.setFileAsset(fa);
    }

    private void maybeApplyImageFilePatch(Long userId, UserIngredient e, Map<String, Object> body) {
        if (!body.containsKey("image_file") && !body.containsKey("imageFile")) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Object raw = body.containsKey("image_file") ? body.get("image_file") : body.get("imageFile");
        if (raw == null) {
            e.setFileAsset(null);
            return;
        }
        FileAssetDto dto = objectMapper.convertValue(raw, FileAssetDto.class);
        e.setFileAsset(fileAssetService.saveFromUploadMetadata(user, dto));
    }

    private String resolveCategoryName(UserIngredient ui) {
        if (ui.getCategoryId() == null) {
            return null;
        }
        return ingredientCategoryRepository.findById(ui.getCategoryId())
                .map(IngredientCategory::getCategoryName)
                .orElse(null);
    }

    private String resolveCategoryCode(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return ingredientCategoryRepository.findById(categoryId)
                .map(IngredientCategory::getCategoryCode)
                .orElse(null);
    }

    private Long validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (!ingredientCategoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryId;
    }

    /**
     * {@code ingredient_master}에 없는 한글 등 입력 시에도 대표 키워드로 카테고리를 추론한다.
     * 마스터 매칭이 우선이며, 이 메서드는 보조용이다.
     */
    private Long inferCategoryFromNameHeuristic(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return null;
        }
        String n = rawName.trim();
        for (String[] row : HEURISTIC_CATEGORY_KEYWORDS) {
            String code = row[0];
            for (int i = 1; i < row.length; i++) {
                if (n.contains(row[i])) {
                    return categoryIdByCategoryCode(code);
                }
            }
        }
        return null;
    }

    private Long categoryIdByCategoryCode(String categoryCode) {
        return ingredientCategoryRepository.findByCategoryCode(categoryCode)
                .map(IngredientCategory::getCategoryId)
                .orElse(null);
    }

    /** {@code ingredient_category.category_code == UNKNOWN} 인 경우 자동 분류 대상으로 본다. */
    private boolean isUnknownCategoryId(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        return ingredientCategoryRepository.findById(categoryId)
                .map(c -> "UNKNOWN".equalsIgnoreCase(c.getCategoryCode()))
                .orElse(false);
    }

    /**
     * 각 행: {@code [category_code, keyword1, keyword2, ...]} — 위에서 아래로, 행 안에서는 앞 키워드 우선.
     * 채소(VEGETABLE)를 육류(MEAT)보다 먼저 두어 "고추" 등과 충돌을 줄인다.
     */
    private static final String[][] HEURISTIC_CATEGORY_KEYWORDS = {
            {"VEGETABLE", "당근", "양파", "감자", "토마토", "마늘", "오이", "배추", "상추", "양상추", "브로콜리", "버섯", "섬초",
                    "피망", "파프리카", "시금치", "무", "순무", "깻잎", "쪽파", "대파", "아스파라거스", "가지",
                    "애호박", "생강", "양배추", "케일", "청경채", "콩나물", "숙주", "미나리", "시래기"},
            {"MEAT", "돼지고기", "돼지", "삼겹살", "목살", "소고기", "쇠고기", "한우", "닭고기", "닭가슴살", "가슴살", "닭", "오리고기",
                    "양고기", "베이컨", "햄", "소세지", "소시지", "육류"},
            {"SEAFOOD", "생선", "연어", "고등어", "새우", "게", "조개", "멸치", "참치", "오징어", "문어", "해산물"},
            {"DAIRY", "우유", "치즈", "버터", "요거트", "요구르트", "두유", "크림"},
            {"GRAIN", "쌀", "밀가루", "빵", "면", "파스타", "라면"},
    };

    private static void validateNameLength(String name) {
        if (name.length() > NAME_MAX_LEN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static void validateUnitLength(String unit) {
        if (unit != null && unit.length() > UNIT_MAX_LEN) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    /** 등록·수정 시 수량은 null 이 아니면 1 이상(0·음수 불가). */
    private static void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(v.toString());
    }

    private static LocalDate parseLocalDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        return LocalDate.parse(v.toString());
    }

    private static int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    /**
     * 식재료 이미지 인식 — FastAPI 비전만 호출한다. Spring 디스크·{@code file_asset} 저장은 하지 않으며,
     * 실제 바이너리는 등록/수정 시 아파치 {@code upload_fridge_image.php} 경로로 올린다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public VisionRecognizeDataDto recognizeIngredientImage(Long userId, MultipartFile file, int topK) {
        userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미지 파일이 필요합니다.");
        }
        FastApiService.validateVisionImageContentType(file.getContentType());
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "이미지를 읽을 수 없습니다.");
        }
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.jpg";
        String ct = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        MultipartFile forVision = new BytesMultipartFile(bytes, orig, ct);

        VisionRecognizeDataDto data = fastApiService.recognizeIngredientImage(forVision, topK);
        data.setImagePersistStatus("SKIPPED");
        data.setFileId(null);
        data.setRecognitionRequestId(null);
        return data;
    }

    public VisionRecognitionStatusDto getRecognitionStatus(Long userId, Long requestId) {
        userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return visionRecognitionQueryService.getByRequestIdAndUser(requestId, userId);
    }
}
