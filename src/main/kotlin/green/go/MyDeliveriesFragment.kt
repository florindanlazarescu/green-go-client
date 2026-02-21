package green.go

import green.go.model.Delivery

class MyDeliveriesFragment : BaseDeliveryFragment() {

    override fun getTitle() = "History"
    
    // În MyDeliveriesFragment am folosit anterior texte directe, aici le-am lăsat momentan pe cele generice 
    // dar le poți schimba în strings.xml dacă dorești ceva specific pentru History.
    override fun getEmptyTitleRes() = R.string.empty_pending_title 
    override fun getEmptyDescRes() = R.string.empty_pending_desc
    override fun getEmptyImageRes() = R.drawable.ic_empty_state
    override fun getAdapterMode() = DeliveryAdapter.MODE_STANDARD

    override fun fetchData(isManualRefresh: Boolean) {
        val id = getCourierId()
        if (id != -1L) {
            viewModel.fetchHistory(id)
        }
    }

    override fun onDeliveryClick(delivery: Delivery) {
        val bottomSheet = DeliveryDetailsBottomSheet(delivery)
        bottomSheet.show(parentFragmentManager, "DeliveryDetailsBottomSheet")
    }
}
