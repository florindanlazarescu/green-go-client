package green.go

import green.go.model.Delivery

class PickedUpFragment : BaseDeliveryFragment() {

    override fun getTitle() = "Picked Up"
    override fun getEmptyTitleRes() = R.string.empty_pickedup_title
    override fun getEmptyDescRes() = R.string.empty_pickedup_desc
    override fun getEmptyImageRes() = R.drawable.picked_up
    override fun getAdapterMode() = DeliveryAdapter.MODE_ACTIVE
    override fun isAutoRefreshEnabled() = true

    override fun fetchData(isManualRefresh: Boolean) {
        val courierId = getCourierId()
        if (courierId != -1L) {
            viewModel.fetchPickedUpDeliveries(courierId)
        }
    }

    override fun onDeliveryClick(delivery: Delivery) {
        if (delivery.status == "PICKED_UP") {
            val bottomSheet = PickDeliveryBottomSheet(
                delivery = delivery,
                buttonText = "Delivered",
                questionText = "Confirm you have successfully delivered this order."
            ) {
                viewModel.updateDeliveryStatus(it, "DELIVERED", getCourierId())
            }
            bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
        }
    }
}
