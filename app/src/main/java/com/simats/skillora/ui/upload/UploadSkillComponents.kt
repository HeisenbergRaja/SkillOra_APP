package com.simats.skillora.ui.upload

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.skillora.ui.theme.GrayGreen
import com.simats.skillora.ui.theme.AvatarBg

@Composable
fun DashedButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            Text(text = label, color = Color.White.copy(alpha = 0.65f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RoadmapCard(
    day: RoadmapDay,
    onToggleExpand: () -> Unit,
    onDeleteDay: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddResource: () -> Unit,
    onEditResource: (ResourceLink) -> Unit,
    onDeleteResource: (String) -> Unit,
    onAddVideo: () -> Unit,
    onEditVideo: (ResourceLink) -> Unit,
    onDeleteVideo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(GrayGreen, RoundedCornerShape(12.dp))
            .clickable { onToggleExpand() }
            .padding(16.dp)
    ) {
        // Collapsed Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Day ${day.dayNumber}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!day.isExpanded) {
                    Text(
                        text = if (day.title.isBlank()) "Enter Day Title" else day.title,
                        color = if (day.title.isBlank()) Color.White.copy(alpha = 0.3f) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${day.fileResources.size} Resources • ${day.videoResources.size} Videos",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }
            }
            Icon(
                imageVector = if (day.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f)
            )
        }

        // Expanded Content
        AnimatedVisibility(
            visible = day.isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Day Title Input
                Text(text = "Day Title", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                OutlinedTextField(
                    value = day.title,
                    onValueChange = onTitleChange,
                    placeholder = { Text("e.g. Introduction to Python", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = AvatarBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Overall Description
                Text(text = "Overall Description", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                OutlinedTextField(
                    value = day.description,
                    onValueChange = onDescriptionChange,
                    placeholder = { Text("Explain what learners will accomplish...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedBorderColor = AvatarBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Resources", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "File / Notes Resources", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                
                day.fileResources.forEach { res ->
                    ResourceItem(
                        res = res,
                        icon = Icons.Default.Description,
                        onEdit = { onEditResource(res) },
                        onDelete = { onDeleteResource(res.id) }
                    )
                }
                
                SmallDashedButton(label = "+ Add Resource", icon = Icons.Default.Add, onClick = onAddResource)

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Video Resources", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))

                day.videoResources.forEach { video ->
                    ResourceItem(
                        res = video,
                        icon = Icons.Default.PlayCircle,
                        onEdit = { onEditVideo(video) },
                        onDelete = { onDeleteVideo(video.id) }
                    )
                }
                
                SmallDashedButton(label = "+ Add Video", icon = Icons.Default.Add, onClick = onAddVideo)

                Spacer(modifier = Modifier.height(24.dp))
                
                // Delete Day Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteDay() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete Day", color = Color.Red.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ResourceItem(res: ResourceLink, icon: ImageVector, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = res.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = res.url, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onEdit) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SmallDashedButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
fun ResourceDialog(
    resource: ResourceLink?,
    type: String, // "File" or "Video"
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(resource?.title ?: "") }
    var url by remember { mutableStateOf(resource?.url ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrayGreen,
        title = { Text(text = if (resource == null) "Add $type" else "Edit $type", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("$type Title", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Enter $type title", color = Color.White.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = AvatarBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                Column {
                    Text("$type URL", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; error = null },
                        placeholder = { Text("Paste URL here", color = Color.White.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = AvatarBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    if (error != null) {
                        Text(text = error!!, color = Color.Red, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        error = "Please enter a title"
                    } else if (url.isBlank() || !android.util.Patterns.WEB_URL.matcher(url).matches()) {
                        error = "Please enter a valid URL"
                    } else {
                        onConfirm(title, url)
                    }
                }
            ) {
                Text("Save", color = AvatarBg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrayGreen,
        title = { Text(text = "Delete Day?", color = Color.White) },
        text = {
            Text(
                text = "Are you sure you want to delete \"$title\"? All resources associated with this day will also be removed.",
                color = Color.White.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
