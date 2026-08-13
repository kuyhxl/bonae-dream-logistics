package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.response.ErrorResponse;
import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.ProductService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductListDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetProductDto;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateProductDto;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchProductDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "상품 관리 API")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/search")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "상품 검색", description = "상품명 키워드와 업체로 상품을 검색하고 페이징하여 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \"}")))
    })
    public ResponseEntity<PageResponseDto<ResSearchProductDto>> searchProducts(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @Parameter(description = "상품명 검색어", example = "삼겹살") @RequestParam(required = false) String keyword,
            @Parameter(description = "업체 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @RequestParam(required = false) UUID companyId) {
        PageResponseDto<ResSearchProductDto> resDto = productService.searchProducts(pageRequestDto, keyword, companyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @GetMapping("/{productId}")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "상품 단건 조회", description = "상품 ID로 상품 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 상품 (PRODUCT_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"상품을 찾을 수 없습니다. \"}")))
    })
    public ResponseEntity<ResGetProductDto> getProduct(
            @Parameter(description = "상품 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID productId) {
        ResGetProductDto resDto = productService.getProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "상품 생성", description = "새로운 상품을 등록하고, 지정한 허브에 초기 재고를 함께 생성한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "상품 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음. HUB_MANAGER/COMPANY_MANAGER가 자신의 허브·업체가 아닌 상품을 생성하려는 경우 등 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \"}"))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패 / 잘못된 가격 또는 수량 (INVALID_INPUT, INVALID_PRICE, INVALID_QUANTITY)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_QUANTITY\",\"message\":\"허용되지 않는 수량입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 업체 / 허브 (COMPANY_NOT_FOUND, HUB_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"COMPANY_NOT_FOUND\",\"message\":\"업체를 찾을 수 없습니다. \"}"))),
            @ApiResponse(responseCode = "409", description = "동일 업체 내 상품명 중복 (PRODUCT_DUPLICATED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"PRODUCT_DUPLICATED\",\"message\":\"이미 존재하는 상품입니다.\"}")))
    })
    public ResponseEntity<ResCreateProductDto> createProduct(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Valid @RequestBody ReqCreateProductDto reqDto) {
        ResCreateProductDto resDto = productService.createProduct(reqDto, UserRole.valueOf(userRole), username);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징하여 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \"}")))
    })
    public ResponseEntity<PageResponseDto<ResGetProductListDto>> getProducts(@ParameterObject @ModelAttribute PageRequestDto pageRequestDto) {
        PageResponseDto<ResGetProductListDto> resDto = productService.getProducts(pageRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @DeleteMapping("/{productId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @Operation(summary = "상품 삭제", description = "상품을 논리 삭제한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음. HUB_MANAGER가 자신의 허브 소속이 아닌 상품을 삭제하려는 경우 등 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품 / 업체 / 사용자 (PRODUCT_NOT_FOUND, COMPANY_NOT_FOUND, USER_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"상품을 찾을 수 없습니다. \"}")))
    })
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Parameter(description = "상품 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID productId) {
        productService.deleteProduct(productId, UserRole.valueOf(userRole), username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "상품 수정", description = "상품 정보를 부분 수정한다. 전달된 필드만 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음. HUB_MANAGER/COMPANY_MANAGER가 자신의 허브·업체가 아닌 상품을 수정하려는 경우 등 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \"}"))),
            @ApiResponse(responseCode = "400", description = "수정할 필드 없음 / 빈 상품명 / 잘못된 가격 (INVALID_INPUT, INVALID_PRICE)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_PRICE\",\"message\":\"허용되지 않는 가격입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품 / 업체 / 사용자 (PRODUCT_NOT_FOUND, COMPANY_NOT_FOUND, USER_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"상품을 찾을 수 없습니다. \"}"))),
            @ApiResponse(responseCode = "409", description = "동일 업체 내 상품명 중복 (PRODUCT_DUPLICATED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"PRODUCT_DUPLICATED\",\"message\":\"이미 존재하는 상품입니다.\"}")))
    })
    public ResponseEntity<ResUpdateProductDto> updateProduct(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Parameter(description = "상품 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID productId,
            @Valid @RequestBody ReqUpdateProductDto reqDto) {
        ResUpdateProductDto resDto = productService.updateProduct(productId, reqDto, UserRole.valueOf(userRole), username);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }
}