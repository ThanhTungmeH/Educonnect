# 💬 Chat Features Documentation

## Overview

This document describes all the enhanced chat features implemented in the EduConnect application. The chat system has been upgraded with modern messaging capabilities including real-time updates, reactions, file sharing, and more.

---

## 🎯 Features List

### 1. Real-time Messaging ⚡

**Description:** Messages auto-refresh every 3 seconds to show new messages from other users without manual refresh.

**Implementation:**
- Uses `ScheduledExecutorService` for periodic updates
- Auto-starts when a conversation is opened
- Stops when switching to another conversation
- Non-blocking background updates

**User Experience:**
- Seamless message delivery
- No flicker during refresh
- Maintains scroll position unless at bottom

---

### 2. Typing Indicator 📝

**Description:** Shows when the other user is typing a message.

**Features:**
- Displays "{User} is typing..." message below chat area
- Automatically sends typing signal when user starts typing
- Auto-hides after 3 seconds of inactivity
- Real-time updates from server

**Implementation:**
- Client sends `SEND_TYPING` command to server
- Server tracks typing status in `typing_status` table
- Regular polling checks typing status every 3 seconds
- Uses timestamp to filter stale typing indicators (>5 seconds old)

---

### 3. Message Reactions 👍❤️😂

**Description:** Add emoji reactions to messages.

**Available Reactions:**
- 👍 Thumbs up
- ❤️ Heart
- 😂 Laughing
- 😮 Surprised
- 😢 Sad
- 🔥 Fire

**How to Use:**
1. Right-click on any message
2. Select "React" from context menu
3. Choose an emoji
4. Only one reaction per user per message

**Implementation:**
- Stored in `message_reactions` table
- Unique constraint prevents duplicate reactions
- Server aggregates reaction counts
- Updates shown in real-time

---

### 4. Image & File Upload 📎

**Description:** Attach and share files/images in chat.

**Supported Formats:**
- **Images:** JPG, PNG, GIF
- **Documents:** PDF, DOCX, XLSX
- **All files** accepted

**Features:**
- Upload to Cloudinary CDN
- Image preview in chat
- File download links
- Upload progress indication

**How to Use:**
1. Click the 📎 (attach) button
2. Select file from your computer
3. File uploads automatically
4. Message sent with file attachment

**Technical Details:**
- Files stored in Cloudinary cloud storage
- `file_url` stored in database
- `message_type` indicates 'image' or 'file'
- Secure URLs provided for downloads

---

### 5. Message Search 🔍

**Description:** Search through conversation history.

**Features:**
- Real-time search as you type
- Highlights matching messages
- Auto-scroll to first match
- Case-insensitive search

**How to Use:**
1. Click the 🔍 (search) button
2. Type search query
3. Matching messages highlighted
4. Click X to clear search

---

### 6. Message Status Indicators ✓✓

**Description:** Visual indicators showing message delivery status.

**Status Types:**
- **○** (sending) - Message being sent
- **✓** (delivered) - Message delivered to server
- **✓✓** (read) - Message read by recipient

**Colors:**
- Gray: Not yet read
- Green: Read by recipient

**Implementation:**
- Status stored in messages table
- Updates in real-time
- Shown only for sent messages

---

### 7. Edit Messages ✏️

**Description:** Edit your own messages after sending.

**Restrictions:**
- Only your own messages
- Within 15 minutes of sending
- Shows "(edited)" indicator

**How to Use:**
1. Right-click on your message
2. Select "Edit"
3. Enter new content
4. Click OK

**Technical Details:**
- Server validates ownership
- Checks time limit (900 seconds)
- Sets `edited = true` flag
- Records `edited_at` timestamp

---

### 8. Delete Messages 🗑️

**Description:** Remove messages from conversation.

**Features:**
- Soft delete (content replaced with "[Message deleted]")
- Only your own messages can be deleted
- Confirmation dialog
- Permanent action

**How to Use:**
1. Right-click on your message
2. Select "Delete"
3. Confirm deletion
4. Message content replaced

**Technical Details:**
- Sets `deleted = true` flag
- Content replaced with placeholder text
- Message still exists in database
- Can be used for moderation

---

### 9. Context Menu (Right-Click) 🖱️

**Description:** Access message actions via right-click menu.

**Available Actions:**
- **React** - Add emoji reaction (6 options)
- **Edit** - Edit message (own messages only, <15 min)
- **Delete** - Delete message (own messages only)

**Implementation:**
- Shows on right-click or long-press
- Actions filtered by permissions
- Context-sensitive menu items

---

### 10. Improved UI/UX 🎨

**Message Bubbles:**
- Gradient backgrounds for sent messages (blue)
- White background for received messages
- Rounded corners (15px radius)
- Smooth shadows on hover
- Maximum width: 400px

**Animations:**
- Fade-in for new messages (300ms)
- Smooth scroll animations
- Hover effects on messages
- Button ripple effects

**Colors:**
- Sent: `#3498db` → `#2980b9` gradient
- Received: `white` with `#e0e0e0` border
- Text: White for sent, `#2c3e50` for received
- Timestamps: `#7f8c8d` gray

**Responsive Elements:**
- Scroll to bottom button (appears when not at bottom)
- Search bar (toggleable)
- Typing indicator (auto-show/hide)
- Message input with emoji picker

---

### 11. Scroll to Bottom Button ⬇️

**Description:** Quick navigation to latest messages.

**Features:**
- Appears when user scrolls up
- Hides when at bottom (>95% scroll)
- Floating button in bottom-right
- Smooth scroll animation

**Styling:**
- Blue circular button
- 50x50 pixels
- White down arrow
- Semi-transparent background

---

### 12. Auto-refresh Conversations List 📋

**Description:** Conversation list updates automatically.

**Features:**
- Updates unread count in real-time
- Reorders by latest message
- Shows last message preview
- Non-intrusive updates

**Update Triggers:**
- After sending a message
- When opening a conversation
- Every auto-refresh cycle

---

## 🔧 Technical Architecture

### Client-Side Components

**ChatController.java:**
- Main controller for chat interface
- Manages UI interactions
- Handles socket communication
- Implements auto-refresh scheduler

**Key Methods:**
- `startAutoRefresh()` - Initiates 3-second refresh cycle
- `onMessageTyping()` - Handles typing events
- `sendTypingStatus()` - Notifies server of typing
- `uploadFile()` - Handles file upload to Cloudinary
- `addReactionToMessage()` - Sends reaction to server
- `editMessage()` - Sends edit request with validation
- `deleteMessage()` - Sends delete request

**Chat.fxml:**
- Enhanced UI layout
- New buttons: attach, emoji, search
- Typing indicator area
- Scroll to bottom button
- Search bar component

### Server-Side Components

**New Commands:**
1. `SendTypingCommand` - Updates typing status
2. `AddReactionCommand` - Stores message reactions
3. `EditMessageCommand` - Edits message content
4. `DeleteMessageCommand` - Soft deletes messages
5. `GetTypingStatusCommand` - Retrieves typing users

**MessageDAO Methods:**
- `saveReaction()` - Stores emoji reaction
- `getReactions()` - Gets reaction counts
- `editMessage()` - Updates message with validation
- `deleteMessage()` - Soft deletes message
- `updateTypingStatus()` - Updates typing state
- `getTypingUsers()` - Gets currently typing users
- `updateMessageStatus()` - Updates delivery status
- `getMessagesWithPagination()` - Loads message pages

### Database Schema

**New Tables:**

```sql
-- message_reactions
CREATE TABLE message_reactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message_id INT NOT NULL,
    user_id INT NOT NULL,
    reaction VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY (message_id, user_id)
);

-- typing_status
CREATE TABLE typing_status (
    user_id INT NOT NULL,
    conversation_id INT NOT NULL,
    is_typing BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, conversation_id)
);
```

**Enhanced Messages Table:**
```sql
ALTER TABLE messages ADD COLUMN:
- status ENUM('sent', 'delivered', 'read')
- message_type ENUM('text', 'image', 'file')
- file_url VARCHAR(500)
- file_name VARCHAR(255)
- edited BOOLEAN DEFAULT FALSE
- deleted BOOLEAN DEFAULT FALSE
- edited_at TIMESTAMP NULL
```

---

## 🚀 Usage Examples

### Sending a Message with Retry

```java
// Client automatically retries up to 3 times on failure
onSendMessage(); // Handles retry logic internally
```

### Adding a Reaction

```java
// User right-clicks message → selects reaction
addReactionToMessage(messageId, "👍");
// Server stores and returns updated counts
```

### Editing a Message

```java
// User right-clicks own message → selects edit
editMessage(messageId, "Updated content");
// Server validates time limit and ownership
```

### File Upload

```java
// User clicks attach button → selects file
uploadFile(selectedFile);
// Uploads to Cloudinary → sends message with URL
```

---

## ⚠️ Important Notes

### Performance Considerations

1. **Auto-refresh:** Uses background threads to avoid UI blocking
2. **Typing status:** Cleaned automatically after 5 seconds
3. **Message cache:** Limited to prevent memory issues
4. **Lazy loading:** Messages loaded on demand with pagination

### Security

1. **Edit permission:** Server validates message ownership
2. **Time limit:** 15-minute window for message editing
3. **Delete permission:** Only message sender can delete
4. **File uploads:** Validated file types and sizes

### Thread Safety

1. All socket operations use proper synchronization
2. UI updates via `Platform.runLater()`
3. Scheduler uses thread-safe ExecutorService
4. Background operations don't block main thread

---

## 🧪 Testing Checklist

- [x] Send message with auto-retry on failure
- [x] Upload and send image
- [x] Upload and send document
- [x] Add reactions to messages
- [x] Edit own message (within 15 minutes)
- [x] Try to edit after 15 minutes (should fail)
- [x] Delete own message
- [x] Try to delete other user's message (should fail)
- [x] Search messages and verify highlighting
- [x] Typing indicator appears when other user types
- [x] Typing indicator disappears after 3 seconds
- [x] Auto-refresh messages every 3 seconds
- [x] Message status updates (sent → delivered → read)
- [x] Scroll to bottom button appears/disappears correctly
- [x] Context menu on right-click message
- [x] Emoji picker inserts emoji into input
- [x] Smooth animations and transitions

---

## 🔄 Future Enhancements

Potential features for future versions:

1. **Voice Messages** - Record and send audio
2. **Video Calls** - Integrated video chat
3. **Message Forwarding** - Share messages to other chats
4. **Group Chats** - Multi-user conversations
5. **Read Receipts** - Detailed read status per user
6. **Message Pinning** - Pin important messages
7. **GIF Integration** - Built-in GIF picker
8. **Stickers** - Custom sticker packs
9. **Message Threading** - Reply to specific messages
10. **Offline Mode** - Queue messages when offline

---

## 📞 Support

For issues or questions about chat features:
- Check the code comments in `ChatController.java`
- Review the database migration script
- Test with the provided test checklist
- Contact the development team

---

## 📝 Changelog

### Version 2.0 (Current)
- ✅ Real-time messaging with auto-refresh
- ✅ Typing indicators
- ✅ Message reactions (6 emojis)
- ✅ Image & file upload via Cloudinary
- ✅ Message search functionality
- ✅ Message status (sent/delivered/read)
- ✅ Edit & delete messages
- ✅ Improved error handling with retry
- ✅ Enhanced UI/UX with animations
- ✅ Context menu (right-click)
- ✅ Scroll to bottom button
- ✅ Emoji picker

### Version 1.0 (Previous)
- Basic text messaging
- Conversation list
- Message history
- Read receipts

---

**Last Updated:** 2025-11-13  
**Author:** EduConnect Development Team
