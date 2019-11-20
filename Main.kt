// this is a little test from busStop class
fun main() {
    val busStop = BusStop()
    for(i in 0 until 60){
        Thread(Runnable{ busStop.passengerArrive() }).start()
    }
    Thread(Runnable {
        busStop.busArrive()
    }).start()
    for(i in 0 until 25){
        Thread(Runnable{ busStop.passengerArrive() }).start()
    }
    Thread.sleep(2000)
    Thread(Runnable {
        busStop.busArrive()
    }).start()
    for(i in 0 until 25){
        Thread(Runnable{ busStop.passengerArrive() }).start()
    }
    Thread.sleep(2000)
    Thread(Runnable {
        busStop.busArrive()
    }).start()

    while (true){

    }
}