package com.github.adamantcheese.model.source.cache

import androidx.annotation.GuardedBy
import com.github.adamantcheese.model.data.descriptor.ChanDescriptor
import com.github.adamantcheese.model.data.descriptor.PostDescriptor
import com.github.adamantcheese.model.data.post.ChanPost
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

// TODO(archives): tests!
class PostsCache(private val maxValueCount: Int) {
    private val mutex = Mutex()
    private val currentValuesCount = AtomicInteger(0)

    @GuardedBy("mutex")
    private val postsCache = mutableMapOf<ChanDescriptor.ThreadDescriptor, MutableMap<PostDescriptor, ChanPost>>()
    private val originalPostsCache = mutableMapOf<ChanDescriptor.ThreadDescriptor, ChanPost>()
    private val accessTimes = mutableMapOf<ChanDescriptor.ThreadDescriptor, Long>()

    suspend fun putIntoCache(postDescriptor: PostDescriptor, post: ChanPost) {
        mutex.withLock {
            val threadDescriptor = post.postDescriptor.getThreadDescriptor()

            if (!postsCache.containsKey(threadDescriptor)) {
                postsCache[threadDescriptor] = mutableMapOf()
            }

            val count = if (!postsCache[threadDescriptor]!!.containsKey(postDescriptor)) {
                currentValuesCount.incrementAndGet()
            } else {
                currentValuesCount.get()
            }

            if (count > maxValueCount) {
                // Evict 1/4 of the cache
                var amountToEvict = (count / 100) * 25
                if (amountToEvict >= postsCache.size) {
                    amountToEvict = postsCache.size - 1
                }

                if (amountToEvict > 0) {
                    evictOld(amountToEvict)
                }
            }

            if (post.isOp) {
                originalPostsCache[threadDescriptor] = post
            }

            accessTimes[threadDescriptor] = System.currentTimeMillis()
            postsCache[threadDescriptor]!![postDescriptor] = post
        }
    }

    suspend fun getPostFromCache(postDescriptor: PostDescriptor, isOP: Boolean): ChanPost? {
        return mutex.withLock {
            val threadDescriptor = postDescriptor.getThreadDescriptor()
            accessTimes[threadDescriptor] = System.currentTimeMillis()

            val post = postsCache[threadDescriptor]?.get(postDescriptor)
                    ?: return@withLock null

            if (isOP) {
                val originalPost = requireNotNull(originalPostsCache[threadDescriptor]) {
                    "Post is OP but it doesn't have it's original post part"
                }

                return merge(post, originalPost)
            }

            return@withLock post
        }
    }

    suspend fun getOriginalPostFromCache(threadDescriptor: ChanDescriptor.ThreadDescriptor): ChanPost? {
        return mutex.withLock {
            accessTimes[threadDescriptor] = System.currentTimeMillis()

            val post = postsCache[threadDescriptor]?.values?.firstOrNull { post -> post.isOp }
                    ?: return@withLock null

            val originalPost = requireNotNull(originalPostsCache[threadDescriptor]) {
                "Post is OP but it doesn't have it's original post part"
            }

            return@withLock merge(post, originalPost)
        }
    }

    suspend fun getPostsFromCache(threadDescriptor: ChanDescriptor.ThreadDescriptor): List<ChanPost> {
        return mutex.withLock {
            accessTimes[threadDescriptor] = System.currentTimeMillis()
            return@withLock postsCache[threadDescriptor]?.values?.toList() ?: emptyList()
        }
    }

    suspend fun getAll(threadDescriptor: ChanDescriptor.ThreadDescriptor): List<ChanPost> {
        return mutex.withLock {
            accessTimes[threadDescriptor] = System.currentTimeMillis()

            return@withLock postsCache[threadDescriptor]?.values?.toList() ?: emptyList()
        }
    }

    private fun evictOld(amountToEvictParam: Int) {
        require(amountToEvictParam > 0) { "amountToEvictParam is too small: $amountToEvictParam" }
        require(mutex.isLocked) { "mutex must be locked!" }

        val keysSorted = accessTimes.entries
                // We will get the latest accessed key in the beginning of the list
                .sortedBy { (_, lastAccessTime) -> lastAccessTime }
                .map { (key, _) -> key }

        val keysToEvict = mutableListOf<ChanDescriptor.ThreadDescriptor>()
        var amountToEvict = amountToEvictParam

        for (key in keysSorted) {
            if (amountToEvict <= 0) {
                break
            }

            val count = postsCache[key]?.size ?: 0

            keysToEvict += key
            amountToEvict -= count
            currentValuesCount.addAndGet(-count)
        }

        if (currentValuesCount.get() < 0) {
            currentValuesCount.set(0)
        }

        if (keysToEvict.isEmpty()) {
            return
        }

        keysToEvict.forEach { key ->
            postsCache.remove(key)?.clear()
            accessTimes.remove(key)
        }
    }

    suspend fun getCachedValuesCount(): Int {
        return mutex.withLock {
            return@withLock currentValuesCount.get()
        }
    }

    private fun merge(post: ChanPost, originalPost: ChanPost): ChanPost? {
        require(originalPost.isOp) { "originalPost is not OP" }
        require(post.isOp) { "post is not OP" }
        require(originalPost.postDescriptor == post.postDescriptor) {
            "post descriptor differ (${originalPost.postDescriptor}, ${post.postDescriptor})"
        }

        return ChanPost(
                databasePostId = post.databasePostId,
                postDescriptor = post.postDescriptor,
                postImages = post.postImages,
                postIcons = post.postIcons,
                replies = originalPost.replies,
                threadImagesCount = originalPost.threadImagesCount,
                uniqueIps = originalPost.uniqueIps,
                lastModified = originalPost.lastModified,
                sticky = originalPost.sticky,
                closed = originalPost.closed,
                archived = originalPost.archived,
                timestamp = post.timestamp,
                name = post.name,
                postComment = post.postComment,
                subject = post.subject,
                tripcode = post.tripcode,
                posterId = post.posterId,
                moderatorCapcode = post.moderatorCapcode,
                isOp = post.isOp,
                isSavedReply = post.isSavedReply
        )
    }
}