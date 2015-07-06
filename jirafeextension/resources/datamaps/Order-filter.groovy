import de.hybris.platform.core.enums.OrderStatus
return (
    !model.originalVersion &&   // Skip copies of changed orders
    model.status in [           // List the statuses we care about
        OrderStatus.CANCELLED,  // Be sure to map them too (in Order.json)!!!
        OrderStatus.PAYMENT_CAPTURED,
    ]
)
