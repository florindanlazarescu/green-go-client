package green.go

import green.go.model.Delivery

class InProgressFragment : BaseDeliveryFragment() {

    override fun getTitle() = "In Progress"
    override fun getEmptyTitleRes() = R.string.empty_inprogress_title
    override fun getEmptyDescRes() = R.string.empty_inprogress_desc
    override fun getEmptyImageRes() = R.drawable.in_progress
    override fun getAdapterMode() = DeliveryAdapter.MODE_ACTIVE
    override fun isAutoRefreshEnabled() = true
    
    // Canal date In Progress
    override fun getStateLiveData() = viewModel.inProgressState

    override fun fetchData(isManualRefresh: Boolean) {
        val courierId = getCourierId()
        if (courierId != -1L) {
            viewModel.fetchInProgressDeliveries(courierId)
        }
    }

    override fun onDeliveryClick(delivery: Delivery) {
        if (delivery.status == "IN_PROGRESS") {
            val bottomSheet = PickDeliveryBottomSheet(
                delivery = delivery,
                buttonText = "Picked Up",
                questionText = "Confirm you have picked up this order from the merchant."
            ) {
                viewModel.updateDeliveryStatus(it, "PICKED_UP", getCourierId())
            }
            bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
        }
    }
}
