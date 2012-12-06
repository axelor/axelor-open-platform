<html>
<head>
<style>

    * {
    	font-family: Arial, Verdana, sans-serif;
    	font-size: 12px;
    }
    
    #loginHeader {
    	margin-top: 50px;
    	
    	font-size : x-large;
    	font-weight : bold;
    	
    	text-align : center;
    	color : #404040;
    }
    
    #loginFooter {
    	margin-top: 75px;
    	
    	text-align : center;
    	color : #404040;
    }
    
    #loginBody {

    }
    
	#loginForm table {
		margin-left: auto;
		margin-right: auto;
		margin-top: 50px;
		border: 2px solid #667589;
		padding: 20px 50px;
		width : 320px;
	}
	
	#loginForm label {
		font-weight: bold;
	}
	
	#loginForm input {
		border: 1px solid #667589;
		width: 100%;
	}
	
	#loginForm input[type=checkbox] {
		width: auto;
		position: relative;
    	top: 2px;
	}
	
</style>
</head>
<body>

<div id="loginHeader">
DEMO
</div>

<div id="loginBody">
	<form id="loginForm" name="loginfom" action="" method="POST">
		<table>
			<tr>
				<td>
				    <label for="username">User</label><br/>
					<input id="username" name="username"></input>
				</td>
			</tr>
			<tr>
				<td>
					<label for="password">Password</label><br/>
					<input id="password" name="password" type="password"></input>
				</td>
			</tr>
			<tr>
				<td align="left">
					<label for="rememberMe">
						<input type="checkbox" id="rememberMe" name="rememberMe"><font size="2">Remember Me</font>
					</label>
				</td>
			</tr>
			<tr>
				<td align="center">
					<button type="submit">Login</button>
				</td>
			</tr>
		</table>
	</form>
</div>

<div id="loginFooter">
	&copy; Axelor. All Rights Reserved.
</div>

</body>
</html>
