/*
 * KurobaEx - *chan browser https://github.com/K1rakishou/Kuroba-Experimental/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.adamantcheese.chan.core.model;

import androidx.annotation.NonNull;

import com.github.adamantcheese.chan.core.manager.PostPreloadedInfoHolder;
import com.github.adamantcheese.chan.core.model.orm.Loadable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChanThread {
    private Loadable loadable;
    // Unmodifiable list of posts. We need it to make this class "thread-safe" (it's actually
    // still not fully thread-safe because Loadable and the Post classes are not thread-safe but
    // there is no easy way to fix them right now) and to avoid copying the whole list of posts
    // every time it is needed somewhere.
    private List<Post> posts;
    private PostPreloadedInfoHolder postPreloadedInfoHolder;

    private boolean closed = false;
    private boolean archived = false;

    public ChanThread(Loadable loadable, List<Post> posts) {
        this.loadable = loadable;
        this.posts = Collections.unmodifiableList(new ArrayList<>(posts));
    }

    public synchronized void setPostPreloadedInfoHolder(PostPreloadedInfoHolder postPreloadedInfoHolder) {
        this.postPreloadedInfoHolder = postPreloadedInfoHolder;
    }

    public synchronized PostPreloadedInfoHolder getPostPreloadedInfoHolder() {
        return postPreloadedInfoHolder;
    }

    public synchronized int getPostsCount() {
        return posts.size();
    }

    public synchronized int getImagesCount() {
        int total = 0;
        for (Post p : posts) {
            if (p.getPostImagesCount() == 0) {
                continue;
            }

            total += p.getPostImagesCount();
        }
        return total;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean isArchived() {
        return archived;
    }

    public synchronized void setClosed(boolean closed) {
        this.closed = closed;
    }

    public synchronized void setArchived(boolean archived) {
        this.archived = archived;
    }

    public synchronized List<Post> getPosts() {
        return posts;
    }

    public synchronized void clearPosts() {
        posts = Collections.unmodifiableList(new ArrayList<>());
    }

    public synchronized void setNewPosts(List<Post> newPosts) {
        this.posts = Collections.unmodifiableList(new ArrayList<>(newPosts));
    }

    /**
     * Not safe! Only use for read-only operations!
     */
    public synchronized Post getOp() {
        return posts.get(0);
    }

    /**
     * For now it is like this because there are a lot of places that will have to be changed to make
     * this safe
     */
    public Loadable getLoadable() {
        return loadable;
    }

    @NonNull
    @Override
    public String toString() {
        return "ChanThread{" +
                "loadable=" + loadable +
                ", closed=" + closed +
                ", archived=" + archived +
                '}';
    }
}
