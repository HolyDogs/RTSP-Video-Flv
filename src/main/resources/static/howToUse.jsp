<%--
  User: jiangsd
  Date: 2019/9/30
  Time: 11:10
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>视频网</title>
    <%--<%@ include file="/common/taglibs.jsp"%>--%>
    <%--<%@ include file="/common/meta.jsp"%>--%>
    <%--    <script type="text/javascript" src="${ctx }/js/hikvisionVideo/hls.js"> </script>--%>
    <script type="text/javascript" src="${ctx }/js/hikvisionVideo/flv.min.js"></script>
    <%--<script type="text/javascript" src="${ctx }/js/hikvisionVideo/hlsVideo.js"> </script>--%>
    <style type="text/css">
        .videoList{width:0;}
        .videoListNormal{border: 0.05rem solid transparent;}
        .videoListItemCheck {border: 0.05rem solid #d5af6e;background: unset;}
        .videoListFour{
            grid-template-rows: repeat(2, calc((100% - 0.2rem)/2));
            grid-template-columns: repeat(2, calc((100% - 0.2rem)/2));
        }
        .videoListOne{
            grid-template-columns: repeat(1, calc(100% - 0.1rem));
            grid-template-rows: repeat(1, calc(100% - 0.1rem));
        }
    </style>
</head>
<body>
<div id="video">
    <div class="videoPage">
        <div class="videoTitleBg">
            <span>视频网</span>
            <span class="el-icon-close" @click="closeDialog"></span>
        </div>
        <div class="videoArea">
            <%--单位数据--%>
            <div class="searchLine">
                <el-input placeholder="快速查找" suffix-icon="el-icon-search" v-model="input1" class="videoSearch"></el-input>
                <div class="videoListTree">
                    <el-tree :props="props"
                             :highlight-current="true"
                             lazy
                             :load="loadNode"
                             :expand-on-click-node="false"
                             @node-click="changeVideo"
                             :filter-node-method="filterNode"
                             ref="tree">
                         <span class="custom-tree-node"  slot-scope="{node, data}">
                            <span>
                                <i v-if="data.type == 'orgType'" class="iconfont ncxf-tree-danwei" style="font-size: 12px"></i>
                                <i v-if="data.type == 'architType'" class="iconfont ncxf-tree-bulid" style="font-size: 12px"></i>
                                <i v-if="data.type == 'floorType'" class="iconfont ncxf-tree-louceng" style="font-size: 12px"></i>
                                <i v-if="data.type == 'cameraType'" class="iconfont ncxf-tree-shipin" style="font-size: 12px"></i>
                                {{data.name}}
                            </span>
                        </span>
                    </el-tree>
                </div>
            </div>
            <%--视频列表--%>
            <div class="videoList">
                <div :class="{videoListOne:sceneIndex=='0',videoListFour:sceneIndex=='1',videoListNine:sceneIndex=='2'}">
                    <div v-for="(videoList,index) in videoLists" class="videoListNormal" :class="{videoListItemCheck:index==videoListIndex}" @click="sceneItemClick(index)">
                        <!-- <div class="videoTitle"><span>● {{videoList.text}}</span></div> -->
                        <!-- <div class="videoObject"> -->
                        <video :id="videoList.id" style="height:100%;object-fit: fill;" autoplay controls muted></video>
                        <%--<object :id="videoList.id" type="application/x-vlc-plugin" events="True" width="100%" height="100%" class="zin">--%>
                        <%--<param name="mrl" value="" />--%>
                        <%--<param name="volume" value="50" />--%>
                        <%--<param name="autoplay" value="true" />--%>
                        <%--<param name="loop" value="false" />--%>
                        <%--<param name="toolbar" value="false" />--%>
                        <%--</object>--%>
                        <!-- </div> -->
                    </div>
                </div>
                <div class="videoListBtnLine">
                    <span class="sceneChangeTitle">场景模式切换</span>
                    <ul class="sceneChangeBtnLine">
                        <li v-for="(sceneList,index) in sceneLists" class="sceneChangeBtn" :class="{sceneChangeBtnCheck:index==sceneIndex}"  @click="sceneClick(index)">{{sceneList.text}}</li>
                    </ul>
                    <%--<el-divider direction="vertical"></el-divider>
                    <li class="sceneChangeBtn">保存预案</li>--%>
                </div>
            </div>
        </div>
    </div>
</div>
<script type="application/javascript" src="${ctx}/js/hikvisionVideo/vlc.js"></script>
<script>
    var videoMap = new Vue({
        el:'#video',
        data(){
            return{
                videoLists: [
                    {indexCode: '', text: '', id: 'video0', url: '',foreignUrl:''},
                    {indexCode: '', text: '', id: 'video1', url: '',foreignUrl:''},
                    {indexCode: '', text: '', id: 'video2', url: '',foreignUrl:''},
                    {indexCode: '', text: '', id: 'video3', url: '',foreignUrl:''}
                ],
                sceneLists:[
                    {text:'1分屏模式'},
                    {text:'4分屏模式'}
                    // {text:'9分屏模式'}
                ],
                sceneIndex: 0,
                videoListIndex:0,
                input1:'',
                orgList:[],//单位树
                //树的数据
                props: {
                    label: "name",
                    isLeaf: "leafed"
                },
                sourceId:'',//来源ID
                sourceType:'',//来源类型
                //播放器
                player:[]
            }
        },
        methods:{
            //关闭弹窗
            closeDialog(){
                window.parent.HomePad.showVideoPage= false;
                //销毁播放器
                for (var i = 0; i < this.player.length; i++) {
                    this.videoDestroy(this.player, i);
                }
            },
            // 加载子树数据的方法
            loadNode(node, resolve) {
                if (node.level === 0) { //加载顶级节点,单位信息
                    axios.get("${ctx}/hikvisonVideo/getFireImportantOrgList",{
                        params:{}
                    }).then(res => {
                        if(res.data.data){
                        // parentNodeItem是自定义的根节点
                        var item = res.data.data;
                        for(var i = 0 ; i<item.length;i++){
                            var map = {
                                id : item[i].id,
                                name : item[i].orgName,
                                type : 'orgType'
                            }
                            this.orgList.push(map);
                        }
                        resolve(this.orgList);
                    }
                });
                }else{
                    var url="";
                    if (node.data.type === "orgType") {//次级节点,建筑
                        url = "${ctx}/hikvisonVideo/getOrgArchitInfo";
                    } else if (node.data.type === "architType") {//楼层
                        url = "${ctx}/hikvisonVideo/getOrgFloorInfo";
                    } else if (node.data.type === "floorType") {//摄像头
                        url = "${ctx}/hikvisonVideo/getOrgCameraInfo";
                    }
                    if(node.data.type != "cameraType") {
                        //查询数据
                        axios.get(url, {
                            params: {
                                parentId: node.data.id
                            }
                        }).then(res => {
                            if (res.data.data) {
                            // parentNodeItem是自定义的根节点
                            var item = res.data.data;
                            resolve(item);
                        }
                    });
                    }else {
                        var item = [];
                        resolve(item);
                    }

                }
            },
            //销毁播放器，暂停线程
            videoDestroy:function(flvPlayer,i){
                if (flvPlayer[i] != null) {
                    flvPlayer[i].unload();
                    flvPlayer[i].destroy();
                    //请求后台停止该视频的转码器线程
                    axios.get("${ctx}/hikvisonVideo/closeTransThread",{
                        params: {
                            //TODO 配置到服务器后改成内网链接
                            videoUrl: this.videoLists[i].foreignUrl
                        },
                    }).then(res => {
                        console.log("停止线程+" + this.videoLists[i].foreignUrl);
                });
                    flvPlayer[i] = null;
                }
            },
            flvVideoInit:function(id, src){
                var self=this;
                self.src=src;
                self.id=id;
                // 设置属性 type播放类型 url播放链接 isLive是否为直播流 hasAudio是否播放声音 hasVideo是否播放视频 enableStashBuffer开启播放器端缓存
                // enableWorker浏览器端开启flvjs的worker，多进程运行flvjs
                self.flvPlayer = flvjs.createPlayer({
                    type: 'flv',
                    url:src,
                    isLive: true,
                    hasAudio: false,
                    hasVideo: true,
                    enableWorker: true,
                    enableStashBuffer: false,
                    stashInitialSize: 128//减少首桢显示等待时长
                },{});
                //获取页面播放器控件
                self.flvPlayer.attachMediaElement(document.getElementById(id));
                self.flvPlayer.load();
                self.flvPlayer.play();
                return self.flvPlayer;
            },
            // 初始化视频
            initVideo(num) {
                let me = this;
                //请求后台获取视频信息
                axios.get("${ctx}/hikvisonVideo/getOrgVideoInfo", {
                    params: {
                        sourceId: this.sourceId,
                        sourceType: this.sourceType
                    }
                }).then(res => {
                    if (res.data.data.length > 0) {
                    if (num == null) {
                        for (var i = 0; i < res.data.data.length; i++) {
                            if (i === me.getScreenNum(me.sceneIndex)) {
                                break;
                            }
                            //清空摄像头
                            me.videoDestroy(me.player,i);
                            //存放内网视频流
                            this.videoLists[i].url = res.data.data[i].caStreamUrl;
                            //存放外网视频流
                            this.videoLists[i].foreignUrl = res.data.data[i].caStreamUrlForeign;
                            // 摄像头名称
                            this.videoLists[i].text = res.data.data[i].caName;
                            //摄像头编号
                            this.videoLists[i].indexCode = res.data.data[i].caCode;
                            //flvjs创建播放器，btoa base64编码保持url长度
                            //TODO 当前使用外网链接进行播放，到时配置服务器后改成内网链接
                            me.player[i] = me.flvVideoInit(me.videoLists[i].id
                                , "${ctx}/hikvisonVideo/open/" + window.btoa(me.videoLists[i].foreignUrl));
                        }
                    } else {
                        //清空单个摄像头
                        me.videoDestroy(me.player,num);
                        //clearOneVideo(num);
                        // 摄像头名称
                        me.videoLists[num].text = res.data.data[0].caName;
                        //摄像头编号
                        me.videoLists[num].indexCode = res.data.data[0].caCode;
                        //存放外网视频流
                        me.videoLists[num].foreignUrl = res.data.data[0].caStreamUrlForeign;
                        //存放内网视频流
                        me.videoLists[num].url = res.data.data[0].caStreamUrl;
                        //flvjs创建播放器，btoa base64编码保持url长度
                        //TODO 当前使用外网链接进行播放，到时配置服务器后改成内网链接
                        me.player[num] = me.flvVideoInit(me.videoLists[num].id
                            , "${ctx}/hikvisonVideo/open/" + window.btoa(me.videoLists[num].foreignUrl));
                    }
                }
            });
            },
            //切换视频
            changeVideo(node,data,value){
                //ID
                this.sourceId = node.id;
                //类型
                this.sourceType = node.type;
                //根据类型刷新视频
                if(node.type == 'orgType' || node.type == 'architType' || node.type == 'floorType'){
                    //清空摄像头
                    //clearVieo();
                    // 重新加载视频
                    this.initVideo();
                }else {
                    //清空单个摄像头
                    //clearOneVideo(this.videoListIndex);
                    // 重新加载视频
                    this.initVideo(this.videoListIndex);
                }
            },
            //切换分屏数
            sceneClick:function(index) {
                //从四分屏切换为一分屏时销毁其它三个播放器
                if (this.sceneIndex === 1 && index === 0) {
                    for (var i = 1; i < this.player.length; i++) {
                        //销毁播放器
                        this.videoDestroy(this.player, i);
                    }
                }
                //从一分屏切换为四分屏时重新加载其它三个视频
                if (this.sceneIndex === 0 && index === 1) {
                    for (var i = 1; i < this.player.length; i++) {
                        //TODO 当前使用外网链接进行播放，到时配置服务器后改成内网链接
                        this.player[i] = this.flvVideoInit(this.videoLists[i].id
                            , "${ctx}/hikvisonVideo/open/" + window.btoa(this.videoLists[i].foreignUrl));
                    }
                }
                //改变分屏数
                this.sceneIndex = index;

            },
            //选中视频
            sceneItemClick:function (index) {
                this.videoListIndex = index;
            },
            //获取当前分屏数
            getScreenNum:function(param) {
                //index选择第一个，为1分屏，选择第二个为4分屏
                if (param === 0) {
                    return 1;
                }else if (param === 1) {
                    return 4;
                }else if (param === 2) {
                    return 9;
                }else{
                    return param;
                }
            },
            //筛选结点
            filterNode(value, data, node) {
                if (!value) {
                    return true
                }
                let level = node.level;
                //这里使用数组存储 只是为了存储值。
                let _array = [];
                //递归查询获取返回的结点
                this.getReturnNode(node, _array, value);
                let result = false;
                _array.forEach(item => {
                    result = result || item
                });
                return result
            },
            //获取放回节点
            getReturnNode(node, _array, value) {
                let isPass = node.data && node.data.name && node.data.name.indexOf(value) !== -1
                isPass ? _array.push(isPass) : '';
                this.index++;
                if (!isPass && node.level != 1 && node.parent) {
                    this.getReturnNode(node.parent, _array, value)
                }
            }
        },
        watch:{
            //监测搜索框的值变化
            input1(val) {
                //筛选树
                this.$refs.tree.filter(val);
            }
        },
        created(){

        },
        mounted:function () {
            // 初始化视频
            this.initVideo();
            // for(var i = 0 ;i<this.videoLists.length;i++){
            //     var videoInfo = this.videoLists[i];
            //     if(i == 0){
            //         // this.loadVideoInfo(videoInfo);
            //     // }else{
            //         setTimeout(this.loadVideoInfo(videoInfo),videoInfo.hlc);
            //     }
            //
            // }
            // this.initVideoInfo();
        }
    });
</script>
</body>
</html>
