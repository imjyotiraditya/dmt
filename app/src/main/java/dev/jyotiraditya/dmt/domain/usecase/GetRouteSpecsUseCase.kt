package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetRouteSpecsUseCase @Inject constructor(
    private val trackMediaRepository: TrackMediaRepository,
    private val dispatchers: DispatcherProvider,
) {
    operator fun invoke(): Flow<List<Spec>> =
        trackMediaRepository.routeSpecs()
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
}
