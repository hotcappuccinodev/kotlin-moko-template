/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package org.example.library.feature.list.presentation

import com.github.aakira.napier.Napier
import dev.icerock.moko.mvvm.ResourceState
import dev.icerock.moko.mvvm.asState
import dev.icerock.moko.mvvm.livedata.LiveData
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import dev.icerock.moko.mvvm.livedata.dataTransform
import dev.icerock.moko.mvvm.livedata.errorTransform
import dev.icerock.moko.mvvm.livedata.map
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.units.TableUnitItem
import kotlinx.coroutines.launch
import org.example.library.feature.list.model.ListSource

class ListViewModel<T>(
    private val listSource: ListSource<T>,
    private val strings: Strings,
    private val unitsFactory: UnitsFactory<T>
) : ViewModel() {

    private val _state: MutableLiveData<ResourceState<List<T>, Throwable>> =
        MutableLiveData(initialValue = ResourceState.Empty())

    val state: LiveData<ResourceState<List<TableUnitItem>, StringDesc>> = _state
        .dataTransform {
            map { news ->
                news.map { unitsFactory.createTile(it) }
            }
        }
        .errorTransform {
            // new type inferrence require set types oO
            map { it.message?.desc() ?: strings.unknownError.desc() }
        }

    fun onCreated() {
        loadList()
    }

    fun onRetryPressed() {
        loadList()
    }

    fun onRefresh(completion: () -> Unit) {
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught") // ktor on ios fail with Throwable when no network
            try {
                val items = listSource.getList()

                _state.value = items.asState()
            } catch (error: Exception) {
                Napier.e("can't refresh", throwable = error)
            } finally {
                completion()
            }
        }
    }

    private fun loadList() {
        _state.value = ResourceState.Loading()

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught") // ktor on ios fail with Throwable when no network
            try {
                val items = listSource.getList()

                _state.value = items.asState()
            } catch (error: Exception) {
                _state.value = ResourceState.Failed(error)
            }
        }
    }

    interface UnitsFactory<T> {
        fun createTile(data: T): TableUnitItem
    }

    interface Strings {
        val unknownError: StringResource
    }
}
