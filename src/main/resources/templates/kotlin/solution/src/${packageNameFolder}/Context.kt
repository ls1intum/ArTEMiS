package ${packageName}

class Context {

    var sortAlgorithm: SortStrategy? = null
    var array: Array<Int>? = null

    fun sort() {
        val arr = this.array ?: error("No array specified!")
        val sortAlgorithm = this.sortAlgorithm ?: error("No sort algorithm specified!")
        sortAlgorithm.performSort(arr)
    }
}
