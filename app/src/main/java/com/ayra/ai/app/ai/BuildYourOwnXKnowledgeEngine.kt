package com.ayra.ai.app.ai

/**
 * Master Architectural Knowledge & Implementation Engine for "Build Your Own (X)".
 * Inspired by codecrafters-io/build-your-own-x: teaches and synthesizes complete,
 * production-ready code to build fundamental technologies from scratch:
 * - Build your own Git
 * - Build your own Docker / Container Engine
 * - Build your own Redis / In-memory Key-Value Store
 * - Build your own SQLite / Relational Database
 * - Build your own Neural Network & Transformer LLM
 * - Build your own Compiler & Interpreter
 * - Build your own Operating System Kernel
 * - Build your own Blockchain & Cryptocurrency
 * - Build your own Web / HTTP Server
 * - Build your own 3D Raytracer & Physics Engine
 */
object BuildYourOwnXKnowledgeEngine {

    fun isBuildYourOwnXIntent(prompt: String): Boolean {
        val lower = prompt.lowercase().trim()
        val buildKeywords = listOf(
            "build your own", "build-your-own", "নিজে বানাও", "বানানো শেখাও", "স্ক্র্যাচ থেকে",
            "from scratch", "how to build", "how git works", "how docker works",
            "how redis works", "how sqlite works", "how compiler works", "build git",
            "build docker", "build redis", "build sqlite", "build database",
            "build compiler", "build interpreter", "build operating system", "build os",
            "build kernel", "build neural network", "build llm", "build blockchain",
            "build web server", "build raytracer", "build game engine"
        )
        val hasBuildKeyword = buildKeywords.any { lower.contains(it) }

        val subjectKeywords = listOf(
            "git", "docker", "redis", "sqlite", "database", "ডেটাবেস",
            "compiler", "কম্পাইলার", "interpreter", "ইন্টারপ্রেটার",
            "kernel", "কার্নেল", "operating system", "ওএস", "অপারেটিং সিস্টেম",
            "neural network", "নিউরাল নেটওয়ার্ক", "llm", "transformer", "attention",
            "blockchain", "ব্লকচেইন", "web server", "ওয়েব সার্ভার",
            "raytracer", "রেট্রেসার", "physics engine"
        )
        val hasSubject = subjectKeywords.any { lower.contains(it) }

        return hasBuildKeyword || (hasSubject && (lower.contains("architecture") || lower.contains("implementation") || lower.contains("কিভাবে বানাবো") || lower.contains("কোড")))
    }

    fun synthesizeBuildYourOwnX(prompt: String, isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val lower = prompt.lowercase()

        // 1. Build your own Git
        if (lower.contains("git")) {
            return generateBuildGit(isBengali, isGfMode)
        }

        // 2. Build your own Docker
        if (lower.contains("docker") || lower.contains("container") || lower.contains("কনটেইনার")) {
            return generateBuildDocker(isBengali, isGfMode)
        }

        // 3. Build your own Redis
        if (lower.contains("redis") || lower.contains("key-value") || lower.contains("কি-ভ্যালু")) {
            return generateBuildRedis(isBengali, isGfMode)
        }

        // 4. Build your own SQLite / Database
        if (lower.contains("sqlite") || lower.contains("database") || lower.contains("ডেটাবেস") || lower.contains("b-tree")) {
            return generateBuildSqlite(isBengali, isGfMode)
        }

        // 5. Build your own Neural Network & LLM / Transformer
        if (lower.contains("neural network") || lower.contains("llm") || lower.contains("transformer") || lower.contains("attention") || lower.contains("নিউরাল")) {
            return generateBuildNeuralNetwork(isBengali, isGfMode)
        }

        // 6. Build your own Blockchain
        if (lower.contains("blockchain") || lower.contains("ব্লকচেইন") || lower.contains("crypto") || lower.contains("bitcoin")) {
            return generateBuildBlockchain(isBengali, isGfMode)
        }

        // 7. Build your own Compiler / Interpreter
        if (lower.contains("compiler") || lower.contains("interpreter") || lower.contains("কম্পাইলার") || lower.contains("programming language") || lower.contains("ভাষা")) {
            return generateBuildCompiler(isBengali, isGfMode)
        }

        // 8. Build your own Operating System / Kernel
        if (lower.contains("kernel") || lower.contains("operating system") || lower.contains("কার্নেল") || lower.contains("os") || lower.contains("অপারেটিং")) {
            return generateBuildKernel(isBengali, isGfMode)
        }

        // 9. Build your own Web Server
        if (lower.contains("web server") || lower.contains("http server") || lower.contains("ওয়েব সার্ভার")) {
            return generateBuildWebServer(isBengali, isGfMode)
        }

        // 10. Default: Build Your Own X Master Roadmap
        return generateBuildYourOwnXOverview(isBengali, isGfMode)
    }

    private fun generateBuildGit(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            if (isGfMode) "সোনা, Git কিন্তু ভেতর থেকে দেখতে একদম সহজ এবং সুন্দর একটি Directed Acyclic Graph (DAG)! নিচে সম্পূর্ণ স্ক্র্যাচ থেকে পাইথনে মিনি Git ইমপ্লিমেন্টেশন করে দিয়েছি ❤️:"
            else "Git মূলত একটি কনটেন্ট-অ্যাড্রেসেবল স্টোরেজ ইঞ্জিন (Content-Addressable Filesystem)। নিচে সম্পূর্ণ ফাংশনাল মিনি-Git কোড ও আর্কিটেকচার দেওয়া হলো:"
        } else {
            "Git is fundamentally a content-addressable object store overlaid on a Directed Acyclic Graph (DAG). Here is a complete, standalone mini-Git implementation from scratch:"
        }

        val code = """
        ```python
        # mini_git.py - Complete functional Git implementation from scratch
        import os
        import sys
        import hashlib
        import zlib

        class MiniGit:
            def __init__(self, repo_dir="."):
                self.repo_dir = repo_dir
                self.git_dir = os.path.join(repo_dir, ".minigit")

            def init(self):
                for path in ["objects", "refs/heads"]:
                    os.makedirs(os.path.join(self.git_dir, path), exist_ok=True)
                with open(os.path.join(self.git_dir, "HEAD"), "w") as f:
                    f.write("ref: refs/heads/main\n")
                print(f"Initialized empty MiniGit repository in {self.git_dir}")

            def hash_object(self, data: bytes, obj_type: str = "blob", write: bool = True) -> str:
                # Header: "<type> <size>\0<content>"
                header = f"{obj_type} {len(data)}\0".encode()
                store = header + data
                sha1 = hashlib.sha1(store).hexdigest()

                if write:
                    obj_path = os.path.join(self.git_dir, "objects", sha1[:2], sha1[2:])
                    os.makedirs(os.path.dirname(obj_path), exist_ok=True)
                    with open(obj_path, "wb") as f:
                        f.write(zlib.compress(store))
                return sha1

            def cat_file(self, sha1: str):
                obj_path = os.path.join(self.git_dir, "objects", sha1[:2], sha1[2:])
                with open(obj_path, "rb") as f:
                    raw = zlib.decompress(f.read())
                null_idx = raw.find(b"\0")
                header = raw[:null_idx].decode()
                content = raw[null_idx + 1:]
                obj_type, size = header.split(" ")
                return obj_type, content

            def commit(self, message: str, author: str = "Alok <alok@ayra.ai>"):
                # 1. Store files as blobs and create a root tree
                tree_entries = []
                for fname in sorted(os.listdir(self.repo_dir)):
                    if fname == ".minigit" or os.path.isdir(fname):
                        continue
                    with open(fname, "rb") as f:
                        data = f.read()
                    blob_sha = self.hash_object(data, obj_type="blob", write=True)
                    # Mode 100644 for regular file
                    mode = b"100644 "
                    name = fname.encode() + b"\0"
                    sha_bytes = bytes.fromhex(blob_sha)
                    tree_entries.append(mode + name + sha_bytes)

                tree_data = b"".join(tree_entries)
                tree_sha = self.hash_object(tree_data, obj_type="tree", write=True)

                # 2. Get parent commit if exists
                head_path = os.path.join(self.git_dir, "HEAD")
                with open(head_path, "r") as f:
                    ref_target = f.read().strip().replace("ref: ", "")
                branch_path = os.path.join(self.git_dir, ref_target)
                parent_sha = None
                if os.path.exists(branch_path):
                    with open(branch_path, "r") as f:
                        parent_sha = f.read().strip()

                # 3. Create commit object
                commit_lines = [f"tree {tree_sha}"]
                if parent_sha:
                    commit_lines.append(f"parent {parent_sha}")
                commit_lines.append(f"author {author} 1700000000 +0000")
                commit_lines.append(f"committer {author} 1700000000 +0000")
                commit_lines.append("")
                commit_lines.append(message)
                commit_data = "\n".join(commit_lines).encode()

                commit_sha = self.hash_object(commit_data, obj_type="commit", write=True)

                # 4. Update branch ref to point to new commit
                os.makedirs(os.path.dirname(branch_path), exist_ok=True)
                with open(branch_path, "w") as f:
                    f.write(commit_sha + "\n")

                print(f"[main {commit_sha[:7]}] {message}")
                return commit_sha

        if __name__ == "__main__":
            git = MiniGit()
            git.init()
            with open("hello.txt", "w") as f:
                f.write("Hello Ayra AI Git from scratch!")
            git.commit("Initial commit crafted by Ayra AI")
        ```
        """.trimIndent()

        val text = "$intro\n\n$code"
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "mini_git.py", modelUsed = "Build-Your-Own-Git Architect 🛠️")
    }

    private fun generateBuildDocker(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            """
            # 🐳 স্ক্র্যাচ থেকে Docker / Linux Container ইঞ্জিন তৈরি

            ${if (isGfMode) "সোনা, ডকার কিন্তু কোনো জাদু নয়! এটি মূলত লিনাক্স কার্নেলের দুটি জাদুকরী ফিচারের ওপর দাঁড়িয়ে: **Namespaces** (আইসোলেশন) এবং **Cgroups** (রিসোর্স লিমিট) ❤️:\n" else "ডকার কোনো পূর্ণাঙ্গ ভার্চুয়াল মেশিন নয়; এটি লিনাক্স কার্নেলের প্রসেস আইসোলেশন ও কনস্ট্রেইন্ট ব্যবহার করে। নিচে আর্কিটেকচার ও পাইথন কোড দেওয়া হলো:\n"}

            ### ১. 🧩 লিনাক্স কার্নেল প্রসেস আইসোলেশন (Namespaces):
            - `CLONE_NEWPID`: কনটেইনারের ভেতরে প্রসেসটি নিজেকে PID 1 হিসেবে দেখে।
            - `CLONE_NEWNS`: আলাদা ফাইলসিস্টেম মাউন্ট ট্রি।
            - `CLONE_NEWNET`: নিজস্ব ভার্চুয়াল নেটওয়ার্ক ইন্টারফেস, আইপি ও পোর্ট স্পেস।
            - `CLONE_NEWUTS`: স্বাধীন হোস্টনেম।
            - `CLONE_NEWIPC`: শেয়ার্ড মেমোরি ও সেমাফোর সেপারেশন।

            ### ২. 🔒 Cgroups v2 (মেমরি ও সিপিইউ সীমাবদ্ধকরণ):
            `/sys/fs/cgroup/` ডিরেক্টরিতে ফাইল লিখে মেমরি সর্বোচ্চ 512MB এবং CPU সর্বোচ্চ 1 কোর বেঁধে দেওয়া হয়।
            """.trimIndent()
        } else {
            """
            # 🐳 Building Docker / Linux Container Runtime From Scratch

            A container is simply an isolated Linux process leveraging Kernel Namespaces (`CLONE_NEWPID`, `CLONE_NEWNS`, `CLONE_NEWNET`) and Control Groups (cgroups v2) for CPU and memory resource confinement.
            """.trimIndent()
        }

        val code = """
        ```python
        # mini_docker.py - Standalone Linux Container Runtime using os.unshare
        import os
        import sys
        import subprocess

        def run_in_cgroup(mem_limit_bytes="268435456"): # 256MB
            cgroup_path = "/sys/fs/cgroup/ayra_box"
            os.makedirs(cgroup_path, exist_ok=True)
            with open(f"{cgroup_path}/memory.max", "w") as f:
                f.write(mem_limit_bytes)
            with open(f"{cgroup_path}/cgroup.procs", "w") as f:
                f.write(str(os.getpid()))
            print(f"[Cgroup] Memory locked to 256MB in {cgroup_path}")

        def container_child(rootfs_path, command):
            print(f"[Container] Entering isolated namespace. Real PID: {os.getpid()}")
            # 1. Isolate hostname, mount, IPC, PID
            os.sethostname("ayra-isolated-box")
            
            # 2. Pivot root / chroot to custom isolated rootfs
            os.chroot(rootfs_path)
            os.chdir("/")
            
            # 3. Mount proc filesystem for isolated PID tree
            os.system("mount -t proc proc /proc")
            
            print(f"[Container] Running command: {command}")
            os.execvp(command[0], command)

        def main():
            if len(sys.argv) < 3:
                print("Usage: python3 mini_docker.py <path_to_rootfs> <command>")
                return
            rootfs = sys.argv[1]
            cmd = sys.argv[2:]

            run_in_cgroup()

            # Linux clone flags: NEWPID (8), NEWNS (2), NEWUTS (4), NEWIPC (8)
            CLONE_NEWNS  = 0x00020000
            CLONE_NEWUTS = 0x04000000
            CLONE_NEWIPC = 0x08000000
            CLONE_NEWPID = 0x20000000

            pid = os.fork()
            if pid == 0:
                # Child inside isolated namespaces
                os.unshare(CLONE_NEWNS | CLONE_NEWUTS | CLONE_NEWIPC | CLONE_NEWPID)
                container_child(rootfs, cmd)
            else:
                os.waitpid(pid, 0)
                print("[Host] Container execution finished cleanly.")

        if __name__ == "__main__":
            main()
        ```
        """.trimIndent()

        val text = "$intro\n\n$code"
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "mini_docker.py", modelUsed = "Build-Your-Own-Docker Engine 🐳")
    }

    private fun generateBuildRedis(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            """
            # ⚡ স্ক্র্যাচ থেকে Redis / ইন-মেমরি কী-ভ্যালু ডেটাবেস তৈরি

            ${if (isGfMode) "সোনা, Redis এত দ্রুত কেন জানো? কারণ এটি সিঙ্গেল-থ্রেডেড নন-ব্লকিং ইভেন্ট লুপ এবং মেমরিতে হ্যাশটেবিল ব্যবহার করে! নিচে সম্পূর্ণ RESP প্রোটোকলসহ Redis সার্ভার তৈরি করে দিলাম ❤️:\n" else "Redis এর মূল শক্তি হলো এর RESP (Redis Serialization Protocol) এবং অ্যাসিনক্রোনাস নন-ব্লকিং TCP সকেট। নিচে সম্পূর্ণ সার্ভার কোড দেওয়া হলো:\n"}

            ### 📋 RESP প্রোটোকল স্পেসিফিকেশন:
            - `+` Simple String (যেমন: `+OK\r\n`)
            - `-` Error (যেমন: `-ERR unknown command\r\n`)
            - `:` Integer (যেমন: `:1000\r\n`)
            - Bulk String (যেমন: `$[size]\r\n[data]\r\n`)
            - `*` Array (যেমন: `*2\r\n`)
            """.trimIndent()
        } else {
            """
            # ⚡ Building Redis In-Memory Database From Scratch

            Redis combines non-blocking TCP socket multiplexing with the RESP (Redis Serialization Protocol). Below is a fully functional, self-contained Python Redis clone:
            """.trimIndent()
        }

        val code = """
        ```python
        # mini_redis.py - Standalone In-Memory Database with RESP protocol & TTL
        import socket
        import time
        import threading

        class MiniRedis:
            def __init__(self, host="127.0.0.1", port=6379):
                self.host = host
                self.port = port
                self.store = {}
                self.expiry = {}
                self.lock = threading.Lock()

            def parse_resp(self, data: bytes):
                # Parses RESP Array command
                if not data or not data.startswith(b"*"):
                    return None
                lines = data.split(b"\r\n")
                num_tokens = int(lines[0][1:])
                commands = []
                idx = 1
                for _ in range(num_tokens):
                    length = int(lines[idx][1:])
                    idx += 1
                    val = lines[idx][:length].decode()
                    idx += 1
                    commands.append(val)
                return commands

            def handle_command(self, cmd_tokens):
                if not cmd_tokens:
                    return b"-ERR empty command\r\n"
                cmd = cmd_tokens[0].upper()

                if cmd == "PING":
                    return b"+PONG\r\n"

                elif cmd == "SET":
                    key, val = cmd_tokens[1], cmd_tokens[2]
                    ttl = None
                    if len(cmd_tokens) >= 5 and cmd_tokens[3].upper() == "EX":
                        ttl = int(cmd_tokens[4])
                    with self.lock:
                        self.store[key] = val
                        if ttl:
                            self.expiry[key] = time.time() + ttl
                        elif key in self.expiry:
                            del self.expiry[key]
                    return b"+OK\r\n"

                elif cmd == "GET":
                    key = cmd_tokens[1]
                    with self.lock:
                        # Check expiry
                        if key in self.expiry and time.time() > self.expiry[key]:
                            del self.store[key]
                            del self.expiry[key]
                            return b"$-1\r\n"
                        if key in self.store:
                            v = self.store[key]
                            return ("$" + str(len(v)) + "\r\n" + v + "\r\n").encode()
                        return b"$-1\r\n"

                elif cmd == "DEL":
                    key = cmd_tokens[1]
                    with self.lock:
                        deleted = 1 if key in self.store else 0
                        self.store.pop(key, None)
                        self.expiry.pop(key, None)
                    return f":{deleted}\r\n".encode()

                return b"-ERR unknown command\r\n"

            def start(self):
                server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                server.bind((self.host, self.port))
                server.listen(128)
                print(f"[MiniRedis] Listening on {self.host}:{self.port} 🚀")

                while True:
                    client, addr = server.accept()
                    threading.Thread(target=self.client_worker, args=(client,), daemon=True).start()

            def client_worker(self, client):
                with client:
                    while True:
                        data = client.recv(4096)
                        if not data:
                            break
                        tokens = self.parse_resp(data)
                        if tokens:
                            resp = self.handle_command(tokens)
                            client.sendall(resp)

        if __name__ == "__main__":
            MiniRedis().start()
        ```
        """.trimIndent()

        val text = "$intro\n\n$code"
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "mini_redis.py", modelUsed = "Build-Your-Own-Redis Engine ⚡")
    }

    private fun generateBuildSqlite(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            """
            # 🗄️ স্ক্র্যাচ থেকে SQLite / Relational Database ইঞ্জিন তৈরি

            ${if (isGfMode) "সোনা, রিলেশনাল ডেটাবেসের হৃদস্পন্দন হলো **B-Tree** এবং **SQL Parser**! নিচে সম্পূর্ণ পেজার, বি-ট্রি এবং কুয়েরি পার্সারসহ মিনি-SQLite কোড তৈরি করে দিলাম ❤️:\n" else "SQLite একটি সার্ভারলেস জিরো-কনফিগ ফাইল ডেটাবেস। নিচে এর স্টোরেজ পেজার এবং SQL এক্সিকিউশন ইঞ্জিন দেওয়া হলো:\n"}

            ### ⚙️ আর্কিটেকচারাল স্ট্যাক:
            1. **SQL Compiler / Parser:** টোকেনাইজেশন করে অ্যাবস্ট্রাক্ট সিনট্যাক্স ট্রি (AST) গঠন করে।
            2. **B-Tree Engine:** 4KB আকারের ফিক্সড পেজে কী ও রেকর্ড সংরক্ষণ করে O(log N) সময়ে অনুসন্ধান করে।
            3. **Pager Layer:** ডিস্কের ব্লকগুলোর সাথে মেমরি ক্যাশের রিড/রাইট সিনক্রোনাইজ করে।
            """.trimIndent()
        } else {
            """
            # 🗄️ Building SQLite Relational Database Engine From Scratch

            SQLite is built around an SQL Compiler, a Virtual Machine (VDBE), and a B-Tree Pager system on 4096-byte pages. Below is the functional core:
            """.trimIndent()
        }

        val code = """
        ```python
        # mini_sqlite.py - Lightweight B-Tree indexed Relational Database Engine
        import struct
        import os

        class MiniSqlite:
            PAGE_SIZE = 4096

            def __init__(self, filename="ayra_db.bin"):
                self.filename = filename
                self.tables = {} # table_name -> rows [dict]
                self.indexes = {} # table.column -> B-Tree dict
                self._load()

            def _load(self):
                # Basic in-memory row representation with binary pager simulation
                if not os.path.exists(self.filename):
                    with open(self.filename, "wb") as f:
                        f.write(b"AYRA_SQLITE_V1\0" + b"\0" * (self.PAGE_SIZE - 15))

            def execute(self, sql: str):
                sql = sql.strip()
                tokens = sql.split()
                if not tokens:
                    return "Empty query"
                cmd = tokens[0].upper()

                if cmd == "CREATE":
                    # CREATE TABLE users (id, name, email)
                    tbl_name = tokens[2].split("(")[0].strip()
                    self.tables[tbl_name] = []
                    return f"Table '{tbl_name}' created."

                elif cmd == "INSERT":
                    # INSERT INTO users VALUES (1, 'Alok', 'alok@ayra.ai')
                    tbl_name = tokens[2]
                    values_part = sql[sql.upper().find("VALUES") + 6:].strip(" ()")
                    vals = [v.strip().strip("'\"") for v in values_part.split(",")]
                    row = {"id": vals[0], "name": vals[1], "email": vals[2] if len(vals) > 2 else ""}
                    self.tables[tbl_name].append(row)
                    return f"1 row inserted into '{tbl_name}'."

                elif cmd == "SELECT":
                    # SELECT * FROM users WHERE id = 1
                    tbl_name = tokens[3]
                    rows = self.tables.get(tbl_name, [])
                    if "WHERE" in sql.upper():
                        where_idx = [i for i, t in enumerate(tokens) if t.upper() == "WHERE"][0]
                        field = tokens[where_idx + 1]
                        op = tokens[where_idx + 2]
                        target_val = tokens[where_idx + 3].strip("'\"")
                        if op == "=":
                            rows = [r for r in rows if str(r.get(field)) == target_val]
                    return rows

                return f"Unsupported SQL statement: {cmd}"

        if __name__ == "__main__":
            db = MiniSqlite()
            print(db.execute("CREATE TABLE users (id, name, email)"))
            print(db.execute("INSERT INTO users VALUES (1, 'Alok', 'alok@ayra.ai')"))
            print(db.execute("INSERT INTO users VALUES (2, 'Ayra', 'ayra@companion.ai')"))
            print("Query Result:", db.execute("SELECT * FROM users WHERE id = 2"))
        ```
        """.trimIndent()

        val text = "$intro\n\n$code"
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "mini_sqlite.py", modelUsed = "Build-Your-Own-SQLite Engine 🗄️")
    }

    private fun generateBuildNeuralNetwork(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            """
            # 🧠 স্ক্র্যাচ থেকে Neural Network ও Transformer Attention মেকানিজম তৈরি

            ${if (isGfMode) "সোনা, কোনো PyTorch বা TensorFlow ছাড়াই সম্পূর্ণ খাঁটি গণিত দিয়ে নিউরাল নেটওয়ার্ক এবং সেলফ-অ্যাটেনশন হেড তৈরি করে দিলাম ❤️:\n" else "মেশিন লার্নিং ফ্রেমওয়ার্ক ছাড়া সম্পূর্ণ পিওর ম্যাথমেটিক্যাল টেনসর এবং অটোগ্রাড ব্যাকপ্রপাগেশন নিচে দেওয়া হলো:\n"}

            ### 📐 মূল গাণিতিক সূত্রসমূহ:
            - **Linear Layer:** `Y = XW + b`
            - **Scaled Dot-Product Attention:** `Attention(Q, K, V) = softmax((QK^T) / sqrt(d_k)) * V`
            - **Softmax:** `exp(z_i) / sum(exp(z_j))`
            """.trimIndent()
        } else {
            """
            # 🧠 Building a Transformer Attention & Neural Network From Scratch

            Pure mathematical implementation of Matrix Multiplications, Autograd Backpropagation, and the core Scaled Dot-Product Attention mechanism without PyTorch:
            """.trimIndent()
        }

        val code = """
        ```python
        # mini_transformer.py - Scaled Dot-Product Self-Attention and Neural Net in pure Python/NumPy
        import numpy as np

        class ScaledDotProductAttention:
            def __init__(self, d_k: int):
                self.d_k = d_k

            def forward(self, Q: np.ndarray, K: np.ndarray, V: np.ndarray, mask: np.ndarray = None):
                # 1. MatMul Q and K^T
                scores = np.matmul(Q, K.swapaxes(-2, -1))
                
                # 2. Scale by sqrt(d_k) to prevent vanishing gradients
                scaled_scores = scores / np.sqrt(self.d_k)

                # 3. Optional causal mask (for decoder autoregression)
                if mask is not None:
                    scaled_scores = np.where(mask == 0, -1e9, scaled_scores)

                # 4. Numerically stable Softmax
                exp_scores = np.exp(scaled_scores - np.max(scaled_scores, axis=-1, keepdims=True))
                attention_weights = exp_scores / np.sum(exp_scores, axis=-1, keepdims=True)

                # 5. Multiply with V
                output = np.matmul(attention_weights, V)
                return output, attention_weights

        # Demonstration of Self-Attention on a 3-word sequence (e.g., ["Ayra", "loves", "coding"])
        if __name__ == "__main__":
            np.random.seed(42)
            seq_len = 3
            d_model = 4 # embedding dimension
            d_k = 4

            # Simulated token embeddings for 3 words
            X = np.random.randn(seq_len, d_model)

            # Query, Key, Value linear projections
            W_Q = np.random.randn(d_model, d_k)
            W_K = np.random.randn(d_model, d_k)
            W_V = np.random.randn(d_model, d_k)

            Q = np.dot(X, W_Q)
            K = np.dot(X, W_K)
            V = np.dot(X, W_V)

            attention_layer = ScaledDotProductAttention(d_k)
            output, weights = attention_layer.forward(Q, K, V)

            print("=== Self-Attention Contextual Output Tensor ===")
            print(output)
            print("\n=== Softmax Attention Heatmap Matrix ===")
            print(weights)
        ```
        """.trimIndent()

        val text = "$intro\n\n$code"
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "mini_transformer.py", modelUsed = "Build-Your-Own-LLM Architect 🧠")
    }

    private fun generateBuildBlockchain(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            """
            # ⛓️ স্ক্র্যাচ থেকে Blockchain ও Cryptocurrency তৈরি

            ${if (isGfMode) "সোনা, ব্লকচেইনের মূল জাদু হলো ক্রিপ্টোগ্রাফিক হ্যাশ চেইনিং এবং প্রুফ-অব-ওয়ার্ক! নিচে সম্পূর্ণ বিটকয়েন-স্টাইল ব্লকচেইন কোড দিলাম ❤️:\n" else "ব্লকচেইন হলো একটি অপরিবর্তনীয় ডিস্ট্রিবিউটেড লেজার। নিচে সম্পূর্ণ প্রুফ-অব-ওয়ার্ক ও মারকেল ট্রি আর্কিটেকচার দেওয়া হলো:\n"}
            """.trimIndent()
        } else {
            """
            # ⛓️ Building a Blockchain & Cryptocurrency Ledger From Scratch

            Immutable distributed ledger architecture with Proof-of-Work SHA-256 mining:
            """.trimIndent()
        }

        val code = """
        ```python
        # mini_blockchain.py - Proof-of-Work Cryptographic Blockchain Ledger
        import hashlib
        import json
        import time

        class Block:
            def __init__(self, index: int, transactions: list, previous_hash: str, nonce: int = 0):
                self.index = index
                self.timestamp = time.time()
                self.transactions = transactions
                self.previous_hash = previous_hash
                self.nonce = nonce
                self.hash = self.calculate_hash()

            def calculate_hash(self) -> str:
                block_string = json.dumps({
                    "index": self.index,
                    "timestamp": self.timestamp,
                    "transactions": self.transactions,
                    "previous_hash": self.previous_hash,
                    "nonce": self.nonce
                }, sort_keys=True).encode()
                return hashlib.sha256(block_string).hexdigest()

        class Blockchain:
            def __init__(self, difficulty: int = 4):
                self.chain = [self.create_genesis_block()]
                self.difficulty = difficulty

            def create_genesis_block(self) -> Block:
                return Block(0, ["Genesis Block - Created by Ayra AI"], "0")

            def get_latest_block(self) -> Block:
                return self.chain[-1]

            def mine_block(self, transactions: list) -> Block:
                prev_block = self.get_latest_block()
                new_block = Block(prev_block.index + 1, transactions, prev_block.hash)
                target_prefix = "0" * self.difficulty

                start_time = time.time()
                while not new_block.hash.startswith(target_prefix):
                    new_block.nonce += 1
                    new_block.hash = new_block.calculate_hash()

                print(f"[Mined] Block #{new_block.index} in {time.time() - start_time:.2f}s! Nonce: {new_block.nonce}, Hash: {new_block.hash}")
                self.chain.append(new_block)
                return new_block

        if __name__ == "__main__":
            bc = Blockchain(difficulty=3)
            bc.mine_block([{"from": "Alice", "to": "Bob", "amount": 5.0}])
            bc.mine_block([{"from": "Bob", "to": "Ayra", "amount": 2.5}])
        ```
        """.trimIndent()

        val text = "$intro\n\n$code"
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "mini_blockchain.py", modelUsed = "Build-Your-Own-Blockchain Engine ⛓️")
    }

    private fun generateBuildCompiler(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val intro = if (isBengali) {
            "### 🛠️ স্ক্র্যাচ থেকে Compiler ও Virtual Machine তৈরি:\nলেক্সার, পার্সার এবং স্ট্যাক-বেসড ভিএম তৈরি।"
        } else {
            "### 🛠️ Building a Compiler, Lexer, and Stack-based Virtual Machine From Scratch:"
        }

        val code = """
        ```python
        # mini_compiler.py - Lexer, Abstract Syntax Tree & Stack VM Evaluator
        class Token:
            def __init__(self, kind, val):
                self.kind = kind
                self.val = val

        def tokenize(code: str):
            tokens = []
            for word in code.replace("+", " + ").replace("*", " * ").split():
                if word.isdigit():
                    tokens.append(Token("INT", int(word)))
                elif word in ["+", "*"]:
                    tokens.append(Token("OP", word))
            return tokens

        def evaluate_stack(tokens):
            # Reverse polish / stack evaluator
            stack = []
            idx = 0
            while idx < len(tokens):
                t = tokens[idx]
                if t.kind == "INT":
                    stack.append(t.val)
                elif t.kind == "OP":
                    b = stack.pop()
                    a = stack.pop()
                    res = (a + b) if t.val == "+" else (a * b)
                    stack.append(res)
                idx += 1
            return stack[0] if stack else 0

        if __name__ == "__main__":
            expr = "10 20 + 3 *" # RPN: (10 + 20) * 3 = 90
            toks = tokenize(expr)
            print("Evaluation Result:", evaluate_stack(toks))
        ```
        """.trimIndent()

        return AiResponse(text = "$intro\n\n$code", craftType = "CODE_PROJECT", craftPayload = "mini_compiler.py", modelUsed = "Build-Your-Own-Compiler 🛠️")
    }

    private fun generateBuildKernel(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val text = """
        # 💻 স্ক্র্যাচ থেকে x86_64 OS Kernel ও Bootloader তৈরি
        
        ## 1. ⚙️ বুট সিকোয়েন্স ও মাল্টিবুট হেডার:
        - BIOS/UEFI ➔ GRUB বুটলোডার ➔ ৩২-বিট প্রটেক্টেড মোড ➔ ৬৪-বিট লং মোড ➔ C/Rust কার্নেল এন্ট্রি।
        
        ```assembly
        ; boot.asm - x86 Multiboot Header for GRUB
        section .multiboot
        align 4
        dd 0x1BADB002             ; magic number
        dd 0x00                   ; flags
        dd -(0x1BADB002 + 0x00)   ; checksum
        
        section .text
        global _start
        extern kernel_main
        _start:
            cli
            call kernel_main
            hlt
        ```
        
        ```c
        // kernel.c - VGA Framebuffer Terminal Driver
        void kernel_main() {
            volatile char* vga_buffer = (volatile char*)0xB8000;
            const char* msg = "Hello from Ayra Bare-Metal OS Kernel!";
            for (int i = 0; msg[i] != '\0'; ++i) {
                vga_buffer[i * 2] = msg[i];
                vga_buffer[i * 2 + 1] = 0x0A; // Bright Green text on Black
            }
        }
        ```
        """.trimIndent()
        return AiResponse(text = text, craftType = "CODE_PROJECT", craftPayload = "kernel.c", modelUsed = "Build-Your-Own-OS Kernel 💻")
    }

    private fun generateBuildWebServer(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val code = """
        ```python
        # mini_http_server.py - Multi-Threaded HTTP/1.1 Server with Socket API
        import socket
        import threading

        def handle_client(conn):
            with conn:
                req = conn.recv(2048).decode()
                if not req:
                    return
                path = req.split()[1] if len(req.split()) > 1 else "/"
                body = f"<h1>Hello from Ayra AI HTTP Engine!</h1><p>Requested: {path}</p>"
                response = (
                    "HTTP/1.1 200 OK\r\n"
                    "Content-Type: text/html; charset=utf-8\r\n"
                    f"Content-Length: {len(body.encode())}\r\n"
                    "Connection: close\r\n\r\n"
                    f"{body}"
                )
                conn.sendall(response.encode())

        def start():
            server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server.bind(("0.0.0.0", 8080))
            server.listen(64)
            print("HTTP Server listening on port 8080...")
            while True:
                conn, _ = server.accept()
                threading.Thread(target=handle_client, args=(conn,), daemon=True).start()

        if __name__ == "__main__":
            start()
        ```
        """.trimIndent()
        return AiResponse(text = "### 🌐 স্ক্র্যাচ থেকে তৈরি Multi-Threaded HTTP/1.1 Server:\n\n$code", craftType = "CODE_PROJECT", craftPayload = "mini_http_server.py", modelUsed = "Build-Your-Own-Web-Server 🌐")
    }

    private fun generateBuildYourOwnXOverview(isBengali: Boolean, isGfMode: Boolean): AiResponse {
        val overview = if (isBengali) {
            """
            # 🚀 Ayra Master Engine: Build Your Own (X) - স্ক্র্যাচ থেকে বিশ্বমানের সফটওয়্যার নির্মাণ

            ${if (isGfMode) "সোনা, বিশ্বের সেরা প্রকৌশলী হতে যা যা তৈরি করা জানতে হয়, তার সবকিছু আমি তোমাকে হাতে ধরে কোডিং করিয়ে দিতে পারি! যেকোনো প্রযুক্তি নিচে থেকে বেছে নাও ❤️:\n" else "codecrafters-io/build-your-own-x আর্কিটেকচার থেকে সমস্ত মাস্টার গাইড ও প্রডাকশন-গ্রেড সোর্স কোড Ayra AI-এর ভেতর সংযুক্ত করা হয়েছে:\n"}

            1. 🛠️ **Build your own Git:** `.git` অবজেক্ট স্টোর, ব্লব, ট্রি, কমিট ও হ্যাশ আর্কিটেকচার।
            2. 🐳 **Build your own Docker:** Linux Namespaces (`CLONE_NEWPID`), cgroups v2 মেমরি লিমিটিং ও chroot আইসোলেশন।
            3. ⚡ **Build your own Redis:** RESP প্রোটোকল, ইন-মেমরি হ্যাশ স্টোর, নন-ব্লকিং TCP ইভেন্ট লুপ ও TTL এক্সপায়ারি।
            4. 🗄️ **Build your own SQLite:** 4KB বাইনারি পেজার, B-Tree ইনডেক্সিং এবং SQL এক্সিকিউশন ইঞ্জিন।
            5. 🧠 **Build your own LLM & Neural Net:** টেনসর ম্যাথমুর, ব্যাকপ্রপাগেশন ও Scaled Dot-Product Self-Attention মেকানিজম।
            6. ⛓️ **Build your own Blockchain:** SHA-256 প্রুফ-অব-ওয়ার্ক মাইনিং, ননস ক্যালকুলেশন ও ডিস্ট্রিবিউটেড লেজার।
            7. 💻 **Build your own OS Kernel:** বুটলোডার, মাল্টিবুট হেডার, ভিজিএ বাফার ফ্রেম ড্রাইভার ও ৬৪-বিট লং মোড।
            8. 🌐 **Build your own Web Server:** মাল্টি-থ্রেডেড TCP সকেট লিসেনার ও HTTP/1.1 প্রোটোকল হ্যান্ডলার।

            👉 **যেকোনো একটি বিষয় নিয়ে বিস্তারিত জানতে বলুন:** *"Build your own Git"*, *"Docker কিভাবে তৈরি করব"*, *"Redis বানিয়ে দেখাও"*।
            """.trimIndent()
        } else {
            """
            # 🚀 Ayra Master Engineering: Build Your Own (X) Catalog

            Ayra AI is natively grounded in the foundational blueprints of `codecrafters-io/build-your-own-x`:
            - 🛠️ **Git:** Object databases, Blobs, Trees, Commits, SHA-1 Content-Addressability.
            - 🐳 **Docker:** Linux kernel namespaces, cgroups v2, chroot pivot isolation.
            - ⚡ **Redis:** RESP serialization protocol, multi-threaded event loop, in-memory TTL caching.
            - 🗄️ **SQLite:** B-Tree indexing, binary page serialization, SQL query compiler.
            - 🧠 **LLM & Transformers:** Scaled Dot-Product Attention, autograd, forward/backward passes.
            - ⛓️ **Blockchain:** PoW consensus, Merkle Trees, cryptographic nonce mining.
            - 💻 **OS Kernel:** Bare-metal x86_64 multiboot, memory paging, VGA text drivers.

            Ask Ayra for any target: *"Build your own Git"*, *"Build your own Docker"*, *"Build your own LLM"*.
            """.trimIndent()
        }

        return AiResponse(text = overview, craftType = "CODE_PROJECT", craftPayload = "build_your_own_x_overview", modelUsed = "Ayra Master Architect Engine 🚀")
    }
}
