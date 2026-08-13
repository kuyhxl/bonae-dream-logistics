package com.bonae.logistics.company.presentation.controller;

import com.bonae.logistics.common.response.PageRequestDto;
import com.bonae.logistics.common.response.PageResponseDto;
import com.bonae.logistics.company.application.CompanyService;
import com.bonae.logistics.company.auth.RoleCheck;
import com.bonae.logistics.company.auth.UserRole;
import com.bonae.logistics.company.presentation.dto.request.ReqCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.request.ReqUpdateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResCreateCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResGetCompanyListDto;
import com.bonae.logistics.company.presentation.dto.response.ResSearchCompanyDto;
import com.bonae.logistics.company.presentation.dto.response.ResUpdateCompanyDto;
import com.bonae.logistics.common.response.ErrorResponse;
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
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Company", description = "업체 관리 API")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @Operation(summary = "업체 생성", description = "새로운 업체(생산업체/수령업체)를 등록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "업체 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패 (INVALID_INPUT)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값이 올바르지 않습니다.\",\"traceId\":\"6a1f3c9d2e4b5a10\",\"fields\":[{\"field\":\"name\",\"reason\":\"업체명은 필수입니다.\"}]}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 허브 (HUB_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"HUB_NOT_FOUND\",\"message\":\"허브를 찾을 수 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "409", description = "동일 업체명+주소 중복 (COMPANY_DUPLICATED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"COMPANY_DUPLICATED\",\"message\":\"이미 동일한 업체명과 주소로 등록된 업체가 존재합니다.\",\"traceId\":\"6a1f3c9d2e4b5a10\"}")))
    })
    public ResponseEntity<ResCreateCompanyDto> createCompany(@Valid @RequestBody ReqCreateCompanyDto reqDto) {
        ResCreateCompanyDto resDto = companyService.createCompany(reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }

    @GetMapping
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "업체 목록 조회", description = "업체 목록을 유형별로 필터링하여 페이징 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 업체 유형 (INVALID_COMPANY_TYPE)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_COMPANY_TYPE\",\"message\":\"유효하지 않은 업체 유형입니다.\",\"traceId\":\"6a1f3c9d2e4b5a10\"}")))
    })
    public ResponseEntity<PageResponseDto<ResGetCompanyListDto>> getCompanies(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @Parameter(description = "업체 유형 필터 (ALL, PRODUCER, RECEIVER)", example = "ALL")
            @RequestParam(defaultValue = "ALL") String type) {
        PageResponseDto<ResGetCompanyListDto> resDto = companyService.getCompanies(pageRequestDto, type);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @GetMapping("/{companyId}")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "업체 단건 조회", description = "업체 ID로 업체 상세 정보를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 업체 (COMPANY_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"COMPANY_NOT_FOUND\",\"message\":\"업체를 찾을 수 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}")))
    })
    public ResponseEntity<ResGetCompanyDto> getCompany(
            @Parameter(description = "업체 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4")
            @PathVariable UUID companyId) {
        ResGetCompanyDto resDto = companyService.getCompany(companyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @GetMapping("/search")
    @RoleCheck({UserRole.MASTER, UserRole.COMPANY_MANAGER, UserRole.HUB_MANAGER, UserRole.DELIVERY_MANAGER})
    @Operation(summary = "업체 검색", description = "업체명 키워드, 유형, 소속 허브로 업체를 검색하고 페이징하여 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 업체 유형 (INVALID_COMPANY_TYPE)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_COMPANY_TYPE\",\"message\":\"유효하지 않은 업체 유형입니다.\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "404", description = "hubId로 전달된 허브가 존재하지 않음 (HUB_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"HUB_NOT_FOUND\",\"message\":\"허브를 찾을 수 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}")))
    })
    public ResponseEntity<PageResponseDto<ResSearchCompanyDto>> searchCompanies(
            @ParameterObject @ModelAttribute PageRequestDto pageRequestDto,
            @Parameter(description = "업체명 검색어", example = "동네반찬") @RequestParam(required = false) String keyword,
            @Parameter(description = "업체 유형 필터 (ALL, PRODUCER, RECEIVER)", example = "ALL") @RequestParam(defaultValue = "ALL") String type,
            @Parameter(description = "소속 허브 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @RequestParam(required = false) UUID hubId) {
        PageResponseDto<ResSearchCompanyDto> resDto = companyService.searchCompanies(pageRequestDto, keyword, type, hubId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @PatchMapping("/{companyId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER, UserRole.COMPANY_MANAGER})
    @Operation(summary = "업체 수정", description = "업체 정보를 부분 수정한다. 전달된 필드만 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음. HUB_MANAGER/COMPANY_MANAGER가 자신의 허브·업체가 아닌 대상을 수정하려는 경우 등 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패 (INVALID_INPUT)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"INVALID_INPUT\",\"message\":\"입력값이 올바르지 않습니다.\",\"traceId\":\"6a1f3c9d2e4b5a10\",\"fields\":[{\"field\":\"address\",\"reason\":\"주소는 255자 이하로 입력해주세요.\"}]}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 업체 / 허브 / 사용자 (COMPANY_NOT_FOUND, HUB_NOT_FOUND, USER_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"COMPANY_NOT_FOUND\",\"message\":\"업체를 찾을 수 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "409", description = "동일 업체명+주소 중복 (COMPANY_DUPLICATED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"COMPANY_DUPLICATED\",\"message\":\"이미 동일한 업체명과 주소로 등록된 업체가 존재합니다.\",\"traceId\":\"6a1f3c9d2e4b5a10\"}")))
    })
    public ResponseEntity<ResUpdateCompanyDto> updateCompany(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Parameter(description = "업체 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID companyId,
            @Valid @RequestBody ReqUpdateCompanyDto reqDto) {
        ResUpdateCompanyDto resDto = companyService.updateCompany(
                companyId, reqDto, UserRole.valueOf(userRole), username);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    @DeleteMapping("/{companyId}")
    @RoleCheck({UserRole.MASTER, UserRole.HUB_MANAGER})
    @Operation(summary = "업체 삭제", description = "업체를 논리 삭제한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (UNAUTHORIZED)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "403", description = "권한 없음. HUB_MANAGER가 자신의 허브 소속이 아닌 업체를 삭제하려는 경우 등 (FORBIDDEN)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"FORBIDDEN\",\"message\":\"해당 작업을 수행할 권한이 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}"))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 업체 / 사용자 (COMPANY_NOT_FOUND, USER_NOT_FOUND)", content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"code\":\"COMPANY_NOT_FOUND\",\"message\":\"업체를 찾을 수 없습니다. \",\"traceId\":\"6a1f3c9d2e4b5a10\"}")))
    })
    public ResponseEntity<Void> deleteCompany(
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") String username,
            @Parameter(description = "업체 ID", example = "3b1c3a78-2b73-4501-bf16-feec87fc98c4") @PathVariable UUID companyId) {
        companyService.deleteCompany(companyId, UserRole.valueOf(userRole), username);
        return ResponseEntity.noContent().build();
    }
}
