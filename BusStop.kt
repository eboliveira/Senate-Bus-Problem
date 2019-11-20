import java.lang.Exception
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class BusStop {
  // number o waiting passenger
  private var lenPassengerWaiting          = 0
  // locks to control this variable
  private val lenPassengerWaitingLock      = ReentrantReadWriteLock()
  private val lenPassengerWaitingWriteLock = lenPassengerWaitingLock.writeLock()
  private val lenPassengerWaitingReadLock  = lenPassengerWaitingLock.readLock()
  //bus on boarding true or false
  private var isBoarding                    = false
  // locks to control this variable
  private val isBoardingLock                = ReentrantReadWriteLock()
  private val isBoardingWriteLock           = isBoardingLock.writeLock()
  private val isBoardingReadLock            = isBoardingLock.readLock()
  // lock to control if the bus can go or not
  private val busCanGo                      = ReentrantLock()
  private val busCanGoCond                  = busCanGo.newCondition()
  // bus that passenger try to acquire
  private val bus                           = Semaphore(0, true)

  // describes the behaviour when a passenger arrives on bus stop
  fun passengerArrive() {
    // check if has another bus on boarding
    // grant read lock
    isBoardingReadLock.lock()
    if(isBoarding) {
      // release the read lock
      isBoardingReadLock.unlock()
      waitForNextBus()
      // lock the read lock again to release above
      isBoardingReadLock.lock()
    }
    isBoardingReadLock.unlock()
    // get the write lock to increment the passenger waiting number
    lenPassengerWaitingWriteLock.lock()
    lenPassengerWaiting += 1
    lenPassengerWaitingWriteLock.unlock()
    // try to enter on bus
    bus.acquire()
    // when get into bus, get write lock and decrement the passengers waiting number
    lenPassengerWaitingWriteLock.lock()
    lenPassengerWaiting -= 1
    lenPassengerWaitingWriteLock.unlock()
    lenPassengerWaitingReadLock.lock()
    // if have no passengers waiting or have no vacancy in bus, the bus can go
    if (lenPassengerWaiting == 0 || bus.availablePermits() == 0) {
      busCanGo.withLock {
        // send the signal enabling the bus leaving
        busCanGoCond.signalAll()
      }
    }
    lenPassengerWaitingReadLock.unlock()
  }

  // describes the behaviour when a bus arrives on bus stop
  fun busArrive() {
    isBoardingWriteLock.lock()
    isBoarding = true
    isBoardingWriteLock.unlock()
    println("Bus arrived")
    // when already set isBoarding, then give 50 vacancy in the bus
    bus.release(50)
    lenPassengerWaitingReadLock.lock()
    // if has no passenger on bus stop, then bus can go, without wait the signal
    if (lenPassengerWaiting > 0) {
      // on other hand, if has passenger in bus stop, then the bus has to wait the signal to go
      lenPassengerWaitingReadLock.unlock()
      busCanGo.withLock {
        busCanGoCond.await()
      }
      lenPassengerWaitingReadLock.lock()
    }
    lenPassengerWaitingReadLock.unlock()
    println("Bus gone with: " + (50 - bus.availablePermits()))
    println("Has " + bus.queueLength + " passengers waiting")
    // when bus is gone, revoke all vacancy and set isBoarding to false
    bus.drainPermits()
    isBoardingWriteLock.lock()
    isBoarding = false
    isBoardingWriteLock.unlock()
  }

  // describes the behaviour when passenger have to wait the next bus, because he arrived when the bus is already on boarding
  private fun waitForNextBus() {
    busCanGo.withLock {
      // just wait the signal saying that the bus can go
      busCanGoCond.await()
    }
  }
}