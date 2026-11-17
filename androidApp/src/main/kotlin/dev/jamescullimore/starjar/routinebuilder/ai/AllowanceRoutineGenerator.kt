package dev.jamescullimore.starjar.routinebuilder.ai

import com.google.firebase.ai.type.PublicPreviewAPI
import dev.jamescullimore.starjar.routinebuilder.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reserve before any cloud-capable SDK request; errors and cancellations retain the reservation. */
@OptIn(PublicPreviewAPI::class)
class AllowanceRoutineGenerator(private val delegate: RoutineGenerator, private val allowance: CloudAllowance) : RoutineGenerator by delegate {
 override suspend fun generate(request: RoutineRequest, policy: GenerationPolicy, cloudDiagnostic: Boolean): GenerationOutput {
  RoutineValidation.request(request)
  if (policy == GenerationPolicy.LOCAL_ONLY) return delegate.generate(request, policy, cloudDiagnostic)
  val reservation = withContext(Dispatchers.IO) { allowance.reserve() }
  val result = delegate.generate(request, policy, cloudDiagnostic)
  if (result.receipt.source == GenerationSource.ON_DEVICE) withContext(Dispatchers.IO) { allowance.refundLocalResult(reservation) }
  return result
 }
}
