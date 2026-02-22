package green.go

import green.go.model.Delivery

class PendingFragment : BaseDeliveryFragment() {

    override fun getTitle() = "Pending"
    override fun getEmptyTitleRes() = R.string.empty_pending_title
    override fun getEmptyDescRes() = R.string.empty_pending_desc
    override fun getEmptyImageRes() = R.drawable.pending
    override fun getAdapterMode() = DeliveryAdapter.MODE_PENDING
    
    // Indica canalul de date pentru Pending
    override fun getStateLiveData() = viewModel.pendingState

    override fun fetchData(isManualRefresh: Boolean) {
        viewModel.fetchPendingDeliveries()
    }

    override fun sortDeliveries(deliveries: List<Delivery>): List<Delivery> {
        return deliveries.sortedBy { parseDate(it.pickUpTime).time }
    }

    override fun onDeliveryClick(delivery: Delivery) {
        val bottomSheet = PickDeliveryBottomSheet(delivery) {
            viewModel.updateDeliveryStatus(it, "IN_PROGRESS", getCourierId())
        }
        bottomSheet.show(parentFragmentManager, "PickDeliveryBottomSheet")
    }
}
