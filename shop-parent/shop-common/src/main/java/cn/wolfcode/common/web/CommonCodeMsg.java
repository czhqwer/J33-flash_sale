package cn.wolfcode.common.web;


public class CommonCodeMsg extends CodeMsg {

    private CommonCodeMsg(Integer code, String msg){
        super(code,msg);
    }
    public static final CommonCodeMsg ILLEGAL_OPERATION = new CommonCodeMsg(-1,"非法操作");
    public static final CommonCodeMsg TOKEN_INVALID = new CommonCodeMsg(-2,"登录超时,请重新登录");
    public static final CommonCodeMsg LOGIN_IP_CHANGE = new CommonCodeMsg(-3,"登录IP发生改变,请重新登录");
    public static final CodeMsg RESULT_INVALID = new CommonCodeMsg(-4, "服务返回数据有异常！");
    public static final CodeMsg  PARAM_INVALID = new CommonCodeMsg(-5, "服务的参数不合法！");
}
