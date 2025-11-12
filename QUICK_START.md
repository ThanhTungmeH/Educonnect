# Quick Start Guide - EduConnect Chat & Friends Features

## Prerequisites
✅ Java 17 installed
✅ MySQL database running
✅ EduConnect database exists

## Step-by-Step Setup

### Step 1: Database Migration (Required First!)

Run the migration script to create the required tables:

```bash
# Option 1: From command line
mysql -u root -p educonnect < src/main/resources/db/migration.sql

# Option 2: Using MySQL Workbench
# - Open MySQL Workbench
# - Connect to your database
# - Open src/main/resources/db/migration.sql
# - Execute the script
```

**Verify tables were created:**
```sql
USE educonnect;
SHOW TABLES;
-- You should see: friends, friend_requests, conversations, conversation_participants, messages
```

### Step 2: Build the Project

```bash
# From project root directory
mvn clean package
```

Expected output: `BUILD SUCCESS`

### Step 3: Start the Server

```bash
# Run the TCP Server
java -cp target/EduConnect1-1.0-SNAPSHOT.jar org.example.educonnect1.Server.TCPServer

# Or use your IDE to run TCPServer.main()
```

You should see:
```
╔════════════════════════════════════════╗
║   EduConnect Server Started!           ║
║   Port: 2005                           ║
║   Status: READY                        ║
╚════════════════════════════════════════╝
```

### Step 4: Run the Client Application

```bash
# Run the JavaFX client
mvn javafx:run

# Or run from your IDE: org.example.educonnect1.Main
```

## Features Overview

### 1. Search for Friends
1. In the main layout, find the search bar at the top
2. Type a user's name (e.g., "John", "Mary")
3. Press Enter or click the search button
4. You'll see a list of matching users
5. Click "Add Friend" to send a friend request

### 2. Manage Friend Requests
1. Click the "Friends" button in the navigation
2. See pending friend requests at the top (with count badge)
3. Click "Accept" to add them as a friend
4. Click "Reject" to decline the request
5. Scroll down to see your current friends list

### 3. Chat with Friends
1. Click the "Messages" button in the navigation
2. On the left: See all your conversations
   - Blue dot indicates unread messages
   - Conversations sorted by most recent
3. Click on a conversation to open it
4. On the right: 
   - See message history
   - Type your message in the input field
   - Click "Send" to send the message

### 4. Start New Conversations
1. Go to Friends list
2. Click "Message" button next to a friend
3. The chat interface will open (enhancement coming soon)

## Testing Checklist

### Test Search Friend
- [ ] Search for existing users by name
- [ ] Verify all matching users appear (not just 1)
- [ ] Send friend request
- [ ] Check database: `SELECT * FROM friend_requests;`

### Test Friend Management
- [ ] Accept a friend request
- [ ] Verify in friends table: `SELECT * FROM friends;`
- [ ] View friends list in UI
- [ ] Reject a friend request

### Test Chat
- [ ] Send a message to a friend
- [ ] Verify in database: `SELECT * FROM messages;`
- [ ] Check conversation appears in list
- [ ] Verify unread count
- [ ] Open conversation and see message
- [ ] Send another message
- [ ] Check messages are in correct order

## Troubleshooting

### Problem: Search returns no results
**Solution:**
- Ensure users exist in database: `SELECT * FROM users;`
- Check server is running and connected
- Verify SearchFriendCommand is in server logs

### Problem: Friend request not working
**Solution:**
- Check friend_requests table: `SELECT * FROM friend_requests;`
- Verify both users exist
- Check for duplicate requests

### Problem: Messages not appearing
**Solution:**
- Run migration script if not done
- Check tables exist: `SHOW TABLES;`
- Verify both users are friends
- Check server logs for errors

### Problem: Database connection error
**Solution:**
- Update `DB.java` with your MySQL credentials:
  ```java
  private static final String URL = "jdbc:mysql://localhost:3306/educonnect";
  private static final String USER = "root";
  private static final String PASSWORD = "your_password";
  ```

## Database Queries for Testing

```sql
-- View all friend relationships
SELECT u1.full_name as user1, u2.full_name as user2
FROM friends f
JOIN users u1 ON f.user_id = u1.id
JOIN users u2 ON f.friend_id = u2.id;

-- View pending friend requests
SELECT u1.full_name as sender, u2.full_name as receiver, fr.status
FROM friend_requests fr
JOIN users u1 ON fr.sender_id = u1.id
JOIN users u2 ON fr.receiver_id = u2.id
WHERE fr.status = 'pending';

-- View conversations with participants
SELECT c.id, u1.full_name as user1, u2.full_name as user2
FROM conversations c
JOIN conversation_participants cp1 ON c.id = cp1.conversation_id
JOIN users u1 ON cp1.user_id = u1.id
JOIN conversation_participants cp2 ON c.id = cp2.conversation_id AND cp2.user_id != cp1.user_id
JOIN users u2 ON cp2.user_id = u2.id;

-- View messages in a conversation
SELECT u.full_name as sender, m.content, m.created_at
FROM messages m
JOIN users u ON m.sender_id = u.id
WHERE m.conversation_id = 1  -- Replace with your conversation ID
ORDER BY m.created_at;
```

## Need Help?

1. Check `FEATURE_DOCUMENTATION.md` for detailed feature documentation
2. Check `IMPLEMENTATION_SUMMARY.md` for technical details
3. Review server logs for error messages
4. Verify database tables and data

## Success Indicators

✅ Server starts without errors
✅ Client connects to server
✅ Can search and find users
✅ Can send friend requests
✅ Can accept/reject requests
✅ Friend list updates correctly
✅ Can send messages
✅ Messages appear in conversation
✅ Unread counts work
✅ Timestamps display correctly

---

**Congratulations!** Your EduConnect application now has full friend management and chat functionality! 🎉
