# Architecture Overview - EduConnect Chat & Friends

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT APPLICATION                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐         │
│  │ MainLayoutCtrl   │  │ SearchFriendCtrl │  │ ChatController   │         │
│  │                  │  │                  │  │                  │         │
│  │ - Search         │  │ - Display results│  │ - Conversations  │         │
│  │ - Navigation     │  │ - Add friends    │  │ - Messages       │         │
│  └──────────────────┘  └──────────────────┘  │ - Send/Receive   │         │
│                                               └──────────────────┘         │
│  ┌──────────────────┐                                                      │
│  │ FriendsListCtrl  │                                                      │
│  │                  │                                                      │
│  │ - Friend requests│                                                      │
│  │ - Friends list   │                                                      │
│  │ - Accept/Reject  │                                                      │
│  └──────────────────┘                                                      │
│           │                                                                 │
│           │ Uses                                                            │
│           ▼                                                                 │
│  ┌──────────────────┐         ┌──────────────────┐                        │
│  │  SocketManager   │────────▶│  SessionManager  │                        │
│  │  (Singleton)     │         │  (Current User)  │                        │
│  └──────────────────┘         └──────────────────┘                        │
│           │                                                                 │
└───────────┼─────────────────────────────────────────────────────────────────┘
            │
            │ TCP Socket Connection (Port 2005)
            │
┌───────────▼─────────────────────────────────────────────────────────────────┐
│                             TCP SERVER                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                         TCPServer                                     │  │
│  │  - Command Map (Action → Command)                                    │  │
│  │  - Client Handlers (Thread Pool)                                     │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│           │                                                                 │
│           │ Dispatches to                                                  │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          COMMANDS                                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                      │   │
│  │  Friend Management:           Messaging:                            │   │
│  │  - SearchFriendCommand        - SendMessageCommand                  │   │
│  │  - AddFriendCommand            - GetConversationsCommand            │   │
│  │  - AcceptFriendCommand         - GetMessagesCommand                 │   │
│  │  - RejectFriendCommand         - MarkMessagesReadCommand            │   │
│  │  - GetFriendsCommand                                                │   │
│  │  - GetFriendRequestsCommand                                         │   │
│  │                                                                      │   │
│  └──────────────────────────────────────────────────────────────────────┘  │
│           │                                                                 │
│           │ Uses                                                            │
│           ▼                                                                 │
│  ┌──────────────────┐         ┌──────────────────┐                        │
│  │   FriendDAO      │         │   MessageDAO     │                        │
│  │                  │         │                  │                        │
│  │ - sendRequest    │         │ - createConv     │                        │
│  │ - acceptRequest  │         │ - saveMessage    │                        │
│  │ - rejectRequest  │         │ - getMessages    │                        │
│  │ - getFriends     │         │ - markAsRead     │                        │
│  │ - areFriends     │         │                  │                        │
│  └──────────────────┘         └──────────────────┘                        │
│           │                              │                                  │
└───────────┼──────────────────────────────┼──────────────────────────────────┘
            │                              │
            │ SQL (Prepared Statements)    │
            ▼                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          MySQL DATABASE                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐  ┌──────────────────┐  ┌──────────────┐                 │
│  │   users     │  │ friend_requests  │  │   friends    │                 │
│  │             │  │                  │  │              │                 │
│  │ - id        │  │ - id             │  │ - id         │                 │
│  │ - email     │  │ - sender_id   ───┼──│ - user_id ───│                 │
│  │ - full_name │  │ - receiver_id ───┘  │ - friend_id  │                 │
│  │ - avatar    │  │ - status         │  │ - created_at │                 │
│  └─────────────┘  └──────────────────┘  └──────────────┘                 │
│                                                                             │
│  ┌────────────────────┐  ┌──────────────────────────┐  ┌─────────────┐   │
│  │  conversations     │  │ conversation_participants│  │  messages   │   │
│  │                    │  │                          │  │             │   │
│  │ - id               │◀─│ - conversation_id        │◀─│ - id        │   │
│  │ - created_at       │  │ - user_id                │  │ - conv_id   │   │
│  │ - updated_at       │  │                          │  │ - sender_id │   │
│  └────────────────────┘  └──────────────────────────┘  │ - content   │   │
│                                                         │ - is_read   │   │
│                                                         │ - created_at│   │
│                                                         └─────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow Examples

### 1. Search Friend Flow
```
User Input → SearchField → MainLayoutController.Search()
                                    ↓
                           SocketManager.sendRequest("SEARCH_FRIEND", query)
                                    ↓
                           TCPServer → SearchFriendCommand
                                    ↓
                           UserDAO.findByName(query)
                                    ↓
                           MySQL: SELECT * FROM users WHERE name LIKE '%query%'
                                    ↓
                           Return List<User> → Client
                                    ↓
                           SearchFriendController.displayResults()
                                    ↓
                           Render User Cards with "Add Friend" buttons
```

### 2. Send Message Flow
```
User Types Message → ChatController.onSendMessage()
                                    ↓
                    SocketManager.sendRequest("SEND_MESSAGE", userId, friendId, content)
                                    ↓
                    TCPServer → SendMessageCommand
                                    ↓
                    MessageDAO.getOrCreateConversation(user1, user2)
                                    ↓
                    MySQL: Check if conversation exists
                           If not: INSERT INTO conversations
                           Then: INSERT INTO conversation_participants
                                    ↓
                    MessageDAO.saveMessage(conversationId, senderId, content)
                                    ↓
                    MySQL: INSERT INTO messages (conversation_id, sender_id, content)
                                    ↓
                    Return "SUCCESS" → Client
                                    ↓
                    Reload Messages → Display in Chat UI
```

### 3. Friend Request Flow
```
User Clicks "Add Friend" → SearchFriendController.handleAddFriend()
                                    ↓
                           SocketManager.sendRequest("ADD_FRIEND", myId, friendId)
                                    ↓
                           TCPServer → AddFriendCommand
                                    ↓
                           FriendDAO.sendFriendRequest(senderId, receiverId)
                                    ↓
                           MySQL: INSERT INTO friend_requests (sender_id, receiver_id)
                                    ↓
                           Return "SUCCESS" → Client
                                    ↓
                           Show confirmation message

                    (Later, friend logs in)
                                    ↓
                    FriendsListController.loadFriendRequests()
                                    ↓
                    SocketManager.sendRequest("GET_FRIEND_REQUESTS", userId)
                                    ↓
                    TCPServer → GetFriendRequestsCommand
                                    ↓
                    FriendDAO.getPendingRequests(userId)
                                    ↓
                    MySQL: SELECT * FROM friend_requests WHERE receiver_id = ?
                                    ↓
                    Return List<Requests> → Display with Accept/Reject buttons

                    (User clicks Accept)
                                    ↓
                    SocketManager.sendRequest("ACCEPT_FRIEND", requestId, userId)
                                    ↓
                    TCPServer → AcceptFriendCommand
                                    ↓
                    FriendDAO.acceptFriendRequest(requestId, userId)
                                    ↓
                    MySQL: INSERT INTO friends (user_id, friend_id) VALUES (...), (...)
                           UPDATE friend_requests SET status = 'accepted'
                                    ↓
                    Return "SUCCESS" → Refresh UI
```

## Security Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Measures                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Authentication                                          │
│     - SessionManager tracks logged-in user                  │
│     - User ID sent with all requests                        │
│                                                             │
│  2. SQL Injection Prevention                                │
│     - All queries use PreparedStatement                     │
│     - No string concatenation in SQL                        │
│                                                             │
│  3. Database Constraints                                    │
│     - Foreign keys enforce referential integrity            │
│     - Unique constraints prevent duplicates                 │
│     - Cascade deletes maintain consistency                  │
│                                                             │
│  4. Authorization                                           │
│     - Commands verify user owns resources                   │
│     - Friend checks before messaging                        │
│                                                             │
│  5. Connection Management                                   │
│     - Try-with-resources for auto-close                     │
│     - Connection pooling ready                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Performance Optimizations

```
┌─────────────────────────────────────────────────────────────┐
│                 Performance Features                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Database Level:                                            │
│  - Indexes on frequently queried columns                    │
│  - Composite indexes for join operations                    │
│  - Limited result sets (LIMIT clauses)                      │
│                                                             │
│  Server Level:                                              │
│  - Thread pool for concurrent clients                       │
│  - Efficient conversation lookup (single query)             │
│  - Reusable DAO instances                                   │
│                                                             │
│  Client Level:                                              │
│  - View caching in MainLayoutController                     │
│  - Lazy image loading for avatars                           │
│  - Background threads for network calls                     │
│  - Platform.runLater() for UI updates                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Component Dependencies

```
Client Controllers
    │
    ├─ Depend on → SocketManager (network communication)
    ├─ Depend on → SessionManager (current user state)
    ├─ Depend on → Model classes (data structures)
    └─ Depend on → FXML views (UI layout)

Server Commands
    │
    ├─ Implement → Command interface
    ├─ Depend on → DAO classes (database access)
    └─ Use → ObjectInputStream/ObjectOutputStream

DAO Classes
    │
    ├─ Depend on → DB utility (connection)
    ├─ Use → PreparedStatement (SQL execution)
    └─ Return → Model classes or Maps

Database Tables
    │
    ├─ Foreign Keys → users table
    ├─ Indexes → performance optimization
    └─ Constraints → data integrity
```

---

This architecture ensures:
- ✅ Clear separation of concerns
- ✅ Scalability (thread pool, indexes)
- ✅ Security (prepared statements, authentication)
- ✅ Maintainability (consistent patterns)
- ✅ Testability (modular components)
