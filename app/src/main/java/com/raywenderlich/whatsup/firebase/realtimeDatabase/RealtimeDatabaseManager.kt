/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.whatsup.firebase.realtimeDatabase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.raywenderlich.whatsup.firebase.authentication.AuthenticationManager
import com.raywenderlich.whatsup.model.Comment
import com.raywenderlich.whatsup.model.Post

private const val POST_REFERENCE       = "posts"
private const val POST_CONTENT_PATH    = "content"
private const val COMMENTS_REFERENCE   = "comments"
private const val COMMENT_POST_ID_PATH = "postId"

class RealtimeDatabaseManager {

  private val authenticationManager = AuthenticationManager()

  private fun getCurrentTime() = System.currentTimeMillis()

  private val database = FirebaseDatabase.getInstance()

  private val postValues = MutableLiveData<List<Post>>()

  private lateinit var postsValueEventListener: ValueEventListener

  private val commentsValues = MutableLiveData<List<Comment>>()

  private lateinit var commentsValueEventListener: ValueEventListener

  private fun createPost(key: String, content: String): Post {
    val user = authenticationManager.getCurrentUser()
    val timestamp = getCurrentTime()
    return Post(key, content, user, timestamp)
  }

  private fun createComment(postId: String, content: String): Comment {
    val user = authenticationManager.getCurrentUser()
    val timestamp = getCurrentTime()
    return Comment(postId, user, timestamp, content)
  }

  fun addPost(content: String, onSuccessAction: () -> Unit, onFailureAction: () -> Unit) {
    val postsReference = database.getReference(POST_REFERENCE)
    val key = postsReference.push().key ?: ""
    val post = createPost(key, content)
    postsReference.child(key)
      .setValue(post)
      .addOnSuccessListener { onSuccessAction() }
      .addOnFailureListener { onFailureAction() }
  }

  fun addComment(postId: String, content: String){
    val commentsReference = database.getReference(COMMENTS_REFERENCE)
    val key = commentsReference.push().key ?: ""
    val comment = createComment(postId, content)
    commentsReference.child(key).setValue(comment)
  }

  private fun listenForPostsValueChanges() {
    postsValueEventListener = object : ValueEventListener {
      override fun onCancelled(p0: DatabaseError) {
        TODO("Not yet implemented")
      }

      override fun onDataChange(p0: DataSnapshot) {
        if (p0.exists()) {
          val posts = p0.children.mapNotNull { it.getValue(Post::class.java) }.toList()
          postValues.postValue(posts)
        }else{
          postValues.postValue(emptyList())
        }
      }
    }
    database.getReference(POST_REFERENCE).addValueEventListener(postsValueEventListener)
  }

  private fun listenForPostCommentsValueChanges(postId: String) {
    commentsValueEventListener = object : ValueEventListener {
      override fun onCancelled(p0: DatabaseError) {
        TODO("Not yet implemented")
      }

      override fun onDataChange(p0: DataSnapshot) {
        if (p0.exists()) {
          val comments = p0.children.mapNotNull { it.getValue(Comment::class.java) }.toList()
          commentsValues.postValue(comments)
        }else {
          commentsValues.postValue(emptyList())
        }
      }
    }
    database.getReference(COMMENTS_REFERENCE)
      .orderByChild(COMMENT_POST_ID_PATH)
      .equalTo(postId)
      .addValueEventListener(commentsValueEventListener)
  }

  private fun deletePostComments(postId: String) {
    database.getReference(COMMENTS_REFERENCE)
      .orderByChild(COMMENT_POST_ID_PATH)
      .equalTo(postId)
      .addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
          TODO("Not yet implemented")
        }

        override fun onDataChange(p0: DataSnapshot) {
          p0.children.forEach { it.ref.removeValue() }
        }
      })

  }

  fun onPostsValuesChange(): LiveData<List<Post>> {
    listenForPostsValueChanges()
    return postValues
  }

  fun onCommentsValuesChange(postId: String): LiveData<List<Comment>> {
    listenForPostCommentsValueChanges(postId)
    return commentsValues
  }

  fun removePostsValuesChangesListener() {
    database.getReference(POST_REFERENCE).removeEventListener(postsValueEventListener)
  }

  fun updatePostContent(key: String, content: String) {
    database.getReference(POST_REFERENCE)
      .child(key)
      .child(POST_CONTENT_PATH)
      .setValue(content)
  }

  fun deletePost(key: String) {
    database.getReference(POST_REFERENCE)
      .child(key)
      .removeValue()
  }


}