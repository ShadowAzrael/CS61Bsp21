# Gitlet Design Document

**Name**:ShadowAzrael

## Classes and Data Structures

### Commit

#### Instance Variables

1. `Message` - Contains the message of a commit.
2. `TimeStamp` - Time at which a commit was created. Assigned by the constructor.
3. `directParent` - The parent commit of a commit.
4. `otherParent` - If merge , use this variable to Record the `branch commit` in ` merge [branchName] ` as the previous node.
5. `HashMap<String, String> blobMap` - The hashMap of file content. The key is the file name of the track file, and the value is the hash name of its corresponding blob.

#### Methods

getters and setters

#### Instance Methods

`getHashName()` - Get the sha-1 hash name of the commmit, sha-1 contains message, time stamp, directParent.

`saveCommit()` - Save object to the `join(COMMIT_FOLER, hashname)`,file name is commit hash name

`addBlob(String fileName, String blobName)` - add blob to the blobMap

`removeBlob(String fileName)` - delete blob from the blobMap

#### Static Methods

`getHeadCommit` - Get commit with HEAD pointer

`getBranchHeadCommit(String branchName, String error_msg)` - Get the Commit object pointed to by the branch file in the branches folder.

`getCommit(String hashName)` - Use hash name to get commit object.

`getCommitFromId(String commitId)` -  Use commitID to get commit object,it supports prefix search.

`getSplitCommit(Commit commitA, Commit commitB)` - Use BFS search commitA and commitB lastest split commit.

### Class 2

#### Fields

1. Field 1
2. Field 2


## Algorithms

## Persistence

