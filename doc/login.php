<?php
// 模拟超时登录退出情况。点击登录，60s不操作，session会失效。
session_start();

if(empty($_SESSION['username'])){	// 用来登录
	$user=$_GET['username'];
	if($user == "admin"){	// 如果用户存在，即用户登录成功
	$_SESSION['username'] = $user;
	$_SESSION['expiretime'] = time() + 60; // 30s内没有接收到请求，session失效
	}
}

if(isset($_SESSION['expiretime'])) {	// 用来控制超时登录退出, 
    if($_SESSION['expiretime'] < time()) {
        unset($_SESSION['expiretime']);
        unset($_SESSION['username']);
        header('Location: login.php?TIMEOUT'); // 登出
        exit(0);
    } else {
         $_SESSION['expiretime'] = time() + 60; // 刷新时间戳
    }
}

?>
<html>
<head>
<title>后台登录</title>
</head>
<body>
	<?php
		if(!empty($_SESSION['username'])){
			echo "用户".$_SESSION['username']."已登录";
		} else {
			echo "当前未登录<br><br><br>";
			echo "<a href='login.php?username=admin'>登录</a>";
		}
	 ?>
</body>
</html>