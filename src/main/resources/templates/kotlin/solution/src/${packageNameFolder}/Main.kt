package ${packageName}

fun main() {
    val sortingContext = Context()
    val policy = Policy(sortingContext)

    var array = Client.createIntegerArray(10)

    for (i in 0..9) {
        array = Client.scrambleArray(array)
        sortingContext.array = array
        Client.simulateRuntimeConfigurationChange(policy)
        print("Unsorted Array a = ")
        Client.printIntegerArray(array)
        sortingContext.sort()
        print("Sorted Array a = ")
        Client.printIntegerArray(array)
    }
}