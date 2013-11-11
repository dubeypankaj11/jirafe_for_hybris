import de.hybris.platform.util.Config
import de.hybris.platform.core.enums.OrderStatus
return (
    !model.originalVersion &&   // Skip copies of changed orders
    model.status in [           // These are the only statuses we care about
        OrderStatus.CANCELLED,
        // Default Hybris 4 doesn't go all the way to COMPLETED
        Config.getParameter("build.version").startsWith('4.') ?
            OrderStatus.PAYMENT_CAPTURED :
            OrderStatus.COMPLETED,
    ]
)
