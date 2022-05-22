//  用户登录操作
layui.form.on('submit(userLogins)', function (res) {
    var formVal = res.field
    //将数据封装成一个JS对象
    var currentParams = {
        phone: formVal.phone,
        password: formVal.passWord
    }
    if(!window.localStorage){ //本地存储
        alert("浏览器不支持localStorage");
        return false;
    }else {
        ajaxHttp({
            url: '/uaa/token',
            type: 'POST',
            data: JSON.stringify(currentParams),
        }, function(params){
            window.localStorage.setItem("token",params.data.token);
            window.localStorage.setItem("userInfo",JSON.stringify(params.data.userInfo));
            window.location.href = "/index.html";
        });
    }
    return false;
})