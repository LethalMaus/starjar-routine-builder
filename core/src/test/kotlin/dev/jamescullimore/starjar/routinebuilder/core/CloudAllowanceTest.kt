package dev.jamescullimore.starjar.routinebuilder.core
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class CloudAllowanceTest {
 private fun scenario(block:(java.io.File)->Unit) { val dir=Files.createTempDirectory("allowance-test").toFile();try{block(java.io.File(dir,"usage.json"))}finally{dir.deleteRecursively()} }
 @Test fun persistsTenReservationsAndRejectsEleventh()=scenario { f ->
  repeat(10){CloudAllowance(f){1000}.reserve()}
  assertEquals(0,CloudAllowance(f){1000}.status().remaining)
  assertThrows(CloudAllowanceExceeded::class.java){CloudAllowance(f){1000}.reserve()}
 }
 @Test fun expiresIndividuallyWithoutMidnightReset()=scenario { f ->
  var now=1000L;val q=CloudAllowance(f){now};q.reserve();now+=1000;q.reserve()
  now=1000+CloudAllowance.WINDOW_MS-1;assertEquals(8,q.status().remaining)
  now++;assertEquals(9,q.status().remaining);now+=1000;assertEquals(10,q.status().remaining)
 }
 @Test fun localRefundIsIdempotentAndDoesNotRefundAnotherRequest()=scenario { f ->
  val q=CloudAllowance(f){1000};val id=q.reserve();q.reserve();q.refundLocalResult(id);q.refundLocalResult(id)
  assertEquals(9,q.status().remaining)
 }
 @Test fun rollbackDoesNotResetAllowance()=scenario { f ->
  var now=100000L;val q=CloudAllowance(f){now};repeat(10){q.reserve()};now=1
  assertEquals(0,q.status().remaining);assertThrows(CloudAllowanceExceeded::class.java){q.reserve()}
 }
 @Test fun corruptStorageFailsClosed()=scenario { f ->
  f.writeText("broken");assertThrows(CloudAllowanceUnavailable::class.java){CloudAllowance(f).reserve()};assertEquals("broken",f.readText())
 }
 @Test fun parallelInstancesCannotExceedTen()=scenario { f ->
  val pool=Executors.newFixedThreadPool(8)
  try {val granted=pool.invokeAll(List(30){Callable {try{CloudAllowance(f){1000}.reserve();true}catch(_:CloudAllowanceExceeded){false}}}).count{it.get()};assertEquals(10,granted)}finally{pool.shutdownNow()}
 }
}
