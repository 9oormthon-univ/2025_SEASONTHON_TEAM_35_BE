package com.growplan.domain.asset.dto;

import com.growplan.global.common.enums.AssetType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class AssetRequestDTO {

    @Getter
    @Setter
    public static class RegisterAssetRequestDTO {

        private List<ItemRequest> assetList;
    }

    @Getter
    @Setter
    public static class ItemRequest {

        private AssetType assetType;
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        @DecimalMax(value = "500000000", inclusive = false, message = "금액은 5억 미만이어야 합니다.")
        private BigDecimal amount;
    }

    @Getter @Setter
    public static class UpdateCashRequest {
        @PositiveOrZero
        @DecimalMax(value = "500000000", inclusive = false, message = "금액은 5억 미만이어야 합니다.")
        private BigDecimal amount;
    }

    @Getter
    @Setter
    public static class UpdateInvestmentsRequest {

        // null은 “변경 없음”, 값이 있으면 해당 금액으로 변경
        @PositiveOrZero @DecimalMax(value = "500000000", inclusive = false)
        private BigDecimal stockAmount;
        @PositiveOrZero @DecimalMax(value = "500000000", inclusive = false)
        private BigDecimal bitcoinAmount;
        @PositiveOrZero @DecimalMax(value = "500000000", inclusive = false)
        private BigDecimal bondAmount;
        @PositiveOrZero @DecimalMax(value = "500000000", inclusive = false)
        private BigDecimal etfAmount;
    }

    // “기타” = CASH/투자 4종을 제외한 타입들
    @Getter
    @Setter
    public static class UpdateOthersRequest {

        private List<AssetRequestDTO.ItemRequest> items; // AssetType + amount
    }
}
