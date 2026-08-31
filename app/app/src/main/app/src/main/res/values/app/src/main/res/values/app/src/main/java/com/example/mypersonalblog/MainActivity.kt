package com.example.mypersonalblog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Post(val id: Long, val image: String, val caption: String, val date: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BlogApp() }
    }
}

@Composable
private fun BlogApp() {
    val context = LocalContext.current
    var posts by remember { mutableStateOf(loadPosts(context)) }
    var name by remember { mutableStateOf(load(context, "name", "My Journal")) }
    var username by remember { mutableStateOf(load(context, "username", "@myjournal")) }
    var bio by remember { mutableStateOf(load(context, "bio", "A little corner of my life.")) }
    var pfp by remember { mutableStateOf(load(context, "pfp", "")) }
    var admin by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    var showPin by remember { mutableStateOf(false) }
    var showProfileEditor by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF101010)) {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
                    if (admin) NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.AddCircle, null) }, label = { Text("Post") })
                }
            }
        ) { pad ->
            when (tab) {
                0 -> Home(posts, name, username, pfp, pad)
                1 -> Profile(posts, name, username, bio, pfp, pad, admin, { showPin = true }, { showProfileEditor = true })
                2 -> if (admin) Editor(pad) { image, caption ->
                    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
                    val post = Post(System.currentTimeMillis(), image, caption, date)
                    posts = listOf(post) + posts
                    savePosts(context, posts)
                    tab = 0
                } else tab = 0
            }
        }

        if (showPin) {
            PinDialog(
                onDismiss = { showPin = false },
                onResult = { ok ->
                    if (ok) {
                        admin = true
                        showPin = false
                        tab = 2
                    }
                }
            )
        }

        if (showProfileEditor && admin) {
            ProfileEditorDialog(
                name = name,
                username = username,
                bio = bio,
                pfp = pfp,
                onDismiss = { showProfileEditor = false },
                onSave = { newName, newUsername, newBio, newPfp ->
                    name = newName
                    username = newUsername
                    bio = newBio
                    pfp = newPfp
                    context.getSharedPreferences("profile", 0).edit()
                        .putString("name", name)
                        .putString("username", username)
                        .putString("bio", bio)
                        .putString("pfp", pfp)
                        .apply()
                    showProfileEditor = false
                }
            )
        }
    }
}

@Composable
private fun Header(name: String, username: String, pfp: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(pfp, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(username, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Avatar(uri: String, size: Dp) {
    if (uri.isNotBlank()) {
        AsyncImage(uri, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF303030)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun Home(posts: List<Post>, name: String, username: String, pfp: String, pad: PaddingValues) {
    LazyColumn(Modifier.padding(pad).fillMaxSize()) {
        item { Header(name, username, pfp) }
        item { Text("My moments", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp, color = Color.Gray) }
        items(posts, key = { it.id }) { PostCard(it, name, pfp, false, {}) }
        if (posts.isEmpty()) item { EmptyState() }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.PhotoLibrary, null, Modifier.size(48.dp), tint = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Text("No posts yet", fontWeight = FontWeight.SemiBold)
        Text("Your first memory will appear here.", color = Color.Gray)
    }
}

@Composable
private fun PostCard(post: Post, name: String, pfp: String, canDelete: Boolean, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(pfp, 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text(post.date, color = Color.Gray, fontSize = 12.sp)
            }
            if (canDelete) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }
        AsyncImage(post.image, null, Modifier.fillMaxWidth().height(340.dp), contentScale = ContentScale.Crop)
        if (post.caption.isNotBlank()) Text(post.caption, Modifier.padding(16.dp, 10.dp, 16.dp, 0), fontSize = 15.sp)
    }
}

@Composable
private fun Profile(
    posts: List<Post>, name: String, username: String, bio: String, pfp: String,
    pad: PaddingValues, admin: Boolean, onUnlock: () -> Unit, onEdit: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf<Post?>(null) }
    Column(Modifier.padding(pad).fillMaxSize()) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(pfp, 88.dp)
            Spacer(Modifier.width(24.dp))
            Column {
                Text(posts.size.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Posts", color = Color.Gray)
            }
        }
        Text(name, Modifier.padding(horizontal = 24.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(username, Modifier.padding(horizontal = 24.dp), color = Color.Gray)
        Text(bio, Modifier.padding(24.dp, 8.dp))
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = if (admin) onEdit else onUnlock) { Icon(if (admin) Icons.Default.Edit else Icons.Default.Lock, null); Spacer(Modifier.width(6.dp)); Text(if (admin) "Edit profile" else "Owner access") }
        }
        HorizontalDivider(Modifier.padding(top = 16.dp))
        Text("Archive", Modifier.padding(24.dp, 18.dp), fontWeight = FontWeight.Bold)
        if (admin) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(posts, key = { it.id }) { post -> PostCard(post, name, pfp, true) { showDeleteConfirm = post } }
            }
        } else {
            Row(Modifier.padding(horizontal = 12.dp)) {
                posts.take(3).forEach { AsyncImage(it.image, null, Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentScale = ContentScale.Crop) }
            }
        }
    }
    showDeleteConfirm?.let { post ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete post?") },
            text = { Text("This will permanently remove the post from this device.") },
            confirmButton = {
                Button(onClick = {
                    val remaining = posts.filterNot { it.id == post.id }
                    savePosts(context, remaining)
                    showDeleteConfirm = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Editor(pad: PaddingValues, onPost: (String, String) -> Unit) {
    val context = LocalContext.current
    var image by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            image = uri.toString()
        }
    }
    Column(Modifier.padding(pad).padding(20.dp)) {
        Text("New post", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Button(onClick = { launcher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text("Choose photo")
        }
        if (image.isNotBlank()) AsyncImage(image, null, Modifier.fillMaxWidth().height(300.dp).padding(top = 16.dp), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(caption, { caption = it }, label = { Text("Caption") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Spacer(Modifier.height(16.dp))
        Button(enabled = image.isNotBlank(), onClick = { onPost(image, caption); image = ""; caption = "" }, modifier = Modifier.fillMaxWidth()) { Text("Publish") }
        Text("Only the owner can access this screen.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onResult: (Boolean) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner access") },
        text = { OutlinedTextField(pin, { pin = it }, singleLine = true, label = { Text("PIN") }) },
        confirmButton = { Button(onClick = { onResult(pin == "1234") }) { Text("Unlock") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileEditorDialog(
    name: String, username: String, bio: String, pfp: String,
    onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit
) {
    var n by remember { mutableStateOf(name) }
    var u by remember { mutableStateOf(username) }
    var b by remember { mutableStateOf(bio) }
    var photo by remember { mutableStateOf(pfp) }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            photo = uri.toString()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { launcher.launch(arrayOf("image/*")) }) { Text("Change profile photo") }
                OutlinedTextField(n, { n = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(u, { u = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(b, { b = it }, label = { Text("Bio") }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { onSave(n, u, b, photo) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun load(c: Context, key: String, def: String): String = c.getSharedPreferences("profile", 0).getString(key, def) ?: def

private fun savePosts(c: Context, posts: List<Post>) {
    val s = posts.joinToString("\n") { listOf(it.id, it.image.replace("|", ""), it.caption.replace("|", ""), it.date).joinToString("|") }
    c.getSharedPreferences("posts", 0).edit().putString("data", s).apply()
}

private fun loadPosts(c: Context): List<Post> {
    val s = c.getSharedPreferences("posts", 0).getString("data", "") ?: ""
    return s.lines().filter { it.isNotBlank() }.mapNotNull { v ->
        val a = v.split("|", limit = 4)
        if (a.size == 4) Post(a[0].toLongOrNull() ?: 0L, a[1], a[2], a[3]) else null
    }
}
