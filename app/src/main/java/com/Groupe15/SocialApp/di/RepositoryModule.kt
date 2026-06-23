package com.Groupe15.SocialApp.di

import com.Groupe15.SocialApp.repository.MessageRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMessageRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): MessageRepository = MessageRepository(firestore, storage)
}