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
    public static class ItemRequest{

        private AssetType assetType;
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        @DecimalMax(value = "500000000", inclusive = false, message = "금액은 5억 미만이어야 합니다.")
        private BigDecimal amount;
    }
}
