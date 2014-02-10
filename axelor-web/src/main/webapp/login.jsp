<%--

    Copyright (c) 2012-2014 Axelor. All Rights Reserved.

    The contents of this file are subject to the Common Public
    Attribution License Version 1.0 (the "License"); you may not use
    this file except in compliance with the License. You may obtain a
    copy of the License at:

    http://license.axelor.com/.

    The License is based on the Mozilla Public License Version 1.1 but
    Sections 14 and 15 have been added to cover use of software over a
    computer network and provide for limited attribution for the
    Original Developer. In addition, Exhibit A has been modified to be
    consistent with Exhibit B.

    Software distributed under the License is distributed on an "AS IS"
    basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
    the License for the specific language governing rights and limitations
    under the License.

    The Original Code is part of "Axelor Business Suite", developed by
    Axelor exclusively.

    The Original Developer is the Initial Developer. The Initial Developer of
    the Original Code is Axelor.

    All portions of the code written by Axelor are
    Copyright (c) 2012-2014 Axelor. All Rights Reserved.

--%>
<!DOCTYPE html>
<html>
<head>
<link href="lib/bootstrap/css/bootstrap.css" rel="stylesheet">
<style type="text/css">

body {
	padding-top: 60px;
	padding-bottom: 40px;
	background-color: #f5f5f5;
}

.form-signin {
	max-width: 300px;
	padding: 19px 29px 29px;
	margin: 0 auto 20px;

	border: 1px solid #e5e5e5;
	background-color: #fff;
	
	-webkit-border-radius: 5px;
  	   -moz-border-radius: 5px;
	        border-radius: 5px;

	-webkit-box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
	   -moz-box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
	        box-shadow: 0 1px 2px rgba(0, 0, 0, .05);
}

.form-signin .form-signin-heading,
.form-signin .checkbox {
	margin-bottom: 10px;
}

.form-signin input[type="text"],
.form-signin input[type="password"] {
	font-size: 16px;
	height: auto;
	margin-bottom: 15px;
	padding: 7px 9px;
}

#footer {
	height: 60px;
}

.container .credit {
	margin: 20px 0;
	text-align: center;
}

.app-title {
	height: 60px;
	font-size: 32px;
	text-align: center;
	margin: 40px 0 20px;
}
</style>
</head>
<body>
	<div class="navbar navbar-inverse navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container">
				<button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
					<span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
				</button>
				<a class="brand" href="http://axelor.com/" style="padding: 3px;">
					<img src="img/axelor.png" style="height: 34px;">
				</a>
				<div class="nav-collapse collapse pull-right">
					<ul class="nav">
						<li class="active"><a href="#">Home</a></li>
						<li><a href="#about">About</a></li>
						<li><a href="#contact">Contact</a></li>
					</ul>
				</div>
				<!--/.nav-collapse -->
			</div>
		</div>
	</div>

	<div class="container">
		<div class="app-title">
			<h2 class="muted">DEMO</h2>
		</div>
		<form class="form-signin" action="" method="POST">
			<h2 class="form-signin-heading">Please sign in</h2>
			<input type="text" class="input-block-level" placeholder="User name"
				tabindex="1" name="username">
			<input type="password" class="input-block-level" placeholder="Password"
				tabindex="2" name="password">
			<label class="checkbox"> <input type="checkbox"
				tabindex="3" value="rememberMe" name="rememberMe"> Remember me
			</label>
			<button tabindex="4" class="btn btn-large btn-primary" type="submit">Login</button>
		</form>
		<div id="footer">
			<div class="container">
				<p class="muted credit">&copy; Axelor. All Rights Reserved.</p>
			</div>
		</div>
	</div>
</body>
</html>
