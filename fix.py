import re

with open('app/src/main/java/com/example/ui/BoteAppUI.kt', 'r') as f:
    content = f.read()

# We need to find the start of the messed up block.
# Look for line 4919 horizontally: "horizontalArrangement = Arrangement.SpaceBetween,"
# In the bad code it's followed by "Moderate Forum" -> { ... App Updates ... } and then the rest of Applicants Status.
# Let's restore from a backup if available. Oh wait, no git.

# I can just replace the whole mess with the correct string.
# I will use regex to find from the start of the first messed up line to the end of the second Moderate Forum block.

start_str = """                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {"""

# Find all occurrences of start_str
idx = content.find(start_str, content.find('items(registrations) { reg ->'))

if idx == -1:
    print("Could not find start_str")
    exit(1)

# Find end of the broken block, which is right before "    // --- DIALOGS FOR EDITING / CREATING SYSTEM DATA ---"
end_str = "    // --- DIALOGS FOR EDITING / CREATING SYSTEM DATA ---"
end_idx = content.find(end_str, idx)

if end_idx == -1:
    print("Could not find end_str")
    exit(1)

good_content = """                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${reg.type} Request", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (reg.status) {
                                                        "Approved" -> SuccessGreen.copy(alpha = 0.15f)
                                                        "Declined" -> ErrorRed.copy(alpha = 0.15f)
                                                        else -> WarningAmber.copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = reg.status,
                                                fontSize = 10.sp,
                                                color = when (reg.status) {
                                                    "Approved" -> SuccessGreen
                                                    "Declined" -> ErrorRed
                                                    else -> WarningAmber
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(reg.applicantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("📞 ${reg.phone} | ✉️ ${reg.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Details: ${reg.details}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.updateRegStatus(reg.id, "Approved") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Approve ✅", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.updateRegStatus(reg.id, "Declined") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Decline ❌", fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.updateRegStatus(reg.id, "Under Review") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Review ⏳", fontSize = 10.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteRegistration(reg.id) },
                                            modifier = Modifier.size(34.dp).background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, "Delete", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Moderate Forum" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "💬 Community Forum Live Moderation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (forumPosts.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text("No messages posted yet in community forum.", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                                }
                            }
                        }

                        items(forumPosts) { post ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${post.author} (${post.authorRole})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(post.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                        Text("Likes: ${post.likes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteForumPost(post.id) },
                                        modifier = Modifier.size(36.dp).background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }

                "App Updates" -> {
                    val appUpdates by viewModel.allAppUpdates.collectAsStateWithLifecycle()
                    var showAddAppUpdateDialog by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📲 Application Version Control",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedButton(onClick = { showAddAppUpdateDialog = true }) {
                                    Text("Post Update", fontSize = 12.sp)
                                }
                            }
                        }
                        if (appUpdates.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text("No app updates posted yet.", modifier = Modifier.padding(16.dp), fontSize = 12.sp)
                                }
                            }
                        }
                        items(appUpdates) { update ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${update.title} (v${update.version})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(update.releaseNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                        if (update.isMandatory) {
                                            Text("⚠️ Mandatory Update", fontSize = 10.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteAppUpdate(update.id) },
                                        modifier = Modifier.size(36.dp).background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }

                    if (showAddAppUpdateDialog) {
                        var updVersion by remember { mutableStateOf("") }
                        var updTitle by remember { mutableStateOf("") }
                        var updNotes by remember { mutableStateOf("") }
                        var isMandatory by remember { mutableStateOf(false) }

                        Dialog(onDismissRequest = { showAddAppUpdateDialog = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Release New App Update", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    
                                    OutlinedTextField(
                                        value = updVersion,
                                        onValueChange = { updVersion = it },
                                        label = { Text("Version (e.g. 1.2.0)", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = updTitle,
                                        onValueChange = { updTitle = it },
                                        label = { Text("Update Title", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = updNotes,
                                        onValueChange = { updNotes = it },
                                        label = { Text("Release Notes", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isMandatory, onCheckedChange = { isMandatory = it })
                                        Text("Force Mandatory Update", fontSize = 12.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showAddAppUpdateDialog = false }) { Text("Cancel") }
                                        Button(onClick = {
                                            viewModel.saveOrUpdateAppUpdate(
                                                AppUpdate(version = updVersion, title = updTitle, releaseNotes = updNotes, isMandatory = isMandatory)
                                            )
                                            showAddAppUpdateDialog = false
                                        }) { Text("Publish Update") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

"""

new_content = content[:idx] + good_content + content[end_idx:]

with open('app/src/main/java/com/example/ui/BoteAppUI.kt', 'w') as f:
    f.write(new_content)

print("Fixed!")
