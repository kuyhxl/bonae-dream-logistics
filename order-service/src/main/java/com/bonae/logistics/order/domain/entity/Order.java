package com.bonae.logistics.order.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import com.bonae.logistics.common.exception.BusinessException;
import com.bonae.logistics.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "requester_company_id", nullable = true)
    private UUID requesterCompanyId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "hub_id")
    private UUID hubId;

    public static Order createPending(
            UUID id,
            UUID requesterCompanyId,
            UUID receiverCompanyId,
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            LocalDateTime dueDate,
            String remarks,
            UUID hubId
    ) {
        Order order = new Order();
        order.id = id;
        order.requesterCompanyId = requesterCompanyId;
        order.receiverCompanyId = receiverCompanyId;
        order.productId = productId;
        order.quantity = quantity;
        order.productName = productName;
        order.unitPrice = unitPrice;
        order.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        order.dueDate = dueDate;
        order.remarks = remarks;
        order.status = OrderStatus.PENDING;
        order.hubId = hubId;
        return order;
    }

    public void cancel(String cancelledBy) {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "PENDING 상태의 주문만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.delete(cancelledBy);
    }

    public void update(LocalDateTime dueDate, String remarks) {
        if(this.status != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "PENDING 상태의 주문만 수정할 수 있습니다.");
        }
        if(dueDate != null) {
           this.dueDate = dueDate;
        }
        if(remarks != null) {
            this.remarks = remarks;
        }
    }
}

