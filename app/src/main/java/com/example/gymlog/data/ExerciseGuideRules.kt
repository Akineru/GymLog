package com.example.gymlog.data

/**
 * 「动作细节」的文案规则表。
 *
 * 数据集有 1283 个动作，逐条手写要点不现实，但同一种动作模式（卧推、划船、弯举…）
 * 的技术要点是一致的。这里按**动作模式**给出四段文案：
 * 发力 / 姿态与幅度 / 常见错误，可选覆盖默认的节奏与呼吸。
 *
 * 只做识别与取词，不做拼接——拼接在 [ExerciseGuideBuilder] 里，方便单测。
 * 关键词都按「去掉空格后的动作名」做包含匹配，与 `DatasetExerciseAliases` 的写法一致。
 */
internal object ExerciseGuideRules {

    /**
     * 一个动作模式。
     *
     * @param key 仅用于测试与排查，不出现在界面文案里。
     * @param keywords 命中任意一个即归入该模式（按列表顺序，先具体后笼统）。
     * @param exclude 命中任意一个则跳过该模式——用来挡掉「腿伸展」里的「拉伸」这类误伤。
     */
    class Movement(
        val key: String,
        val keywords: List<String>,
        val drive: String,
        val posture: String,
        val errors: String,
        val exclude: List<String> = emptyList(),
        val tempo: String? = null,
    )

    /**
     * 顺序即优先级：先匹配具体模式，再匹配笼统模式。
     *
     * 「拉伸」放在最后，因为它的关键词（伸展）会和「肱三头肌伸展」「背伸展」撞车，
     * 那两个模式排在它前面时会被先认走。
     */
    val movements: List<Movement> = listOf(
        Movement(
            key = "卧推",
            keywords = listOf("卧推", "推胸", "胸部 推举"),
            drive = "胸大肌主导，推起时想象把凳子推离自己，顶端主动夹胸半秒。",
            posture = "仰卧平凳，双脚踩实，头、上背、臀三点贴住凳面；下放到杠铃轻触胸骨下缘，上推到肘接近伸直。",
            errors = "肘部外展接近 90° 让肩前侧吃力；臀部离凳或脚蹬地借力；顶端锁死肘关节。",
        ),
        Movement(
            key = "俯卧撑",
            keywords = listOf("俯卧撑"),
            drive = "胸、肩、肱三头一起发力，想象把地面推离身体。",
            posture = "手在肩正下方略宽于肩，身体从头到脚跟成一条直线；胸口下放到离地一拳，上推到肘接近伸直。",
            errors = "塌腰或撅臀；只做上半程；肘部完全外展摊成「T」字。",
        ),
        Movement(
            key = "飞鸟",
            keywords = listOf("飞鸟", "夹胸", "扩胸", "交叉 拉"),
            drive = "全程用胸大肌「环抱」，肘角固定不变，顶端夹紧半秒。",
            posture = "躺或坐稳，肩胛骨下沉后缩；手臂微屈向两侧打开到胸部有拉伸感，再沿弧线抱回胸前。",
            errors = "肘角越做越小变成卧推；耸肩让斜方肌抢力；重量过大靠身体前后晃。",
        ),
        Movement(
            key = "仰卧屈臂上拉",
            keywords = listOf("仰卧屈臂上拉", "上拉"),
            drive = "背阔肌主导，肩关节做大幅度伸展，肋骨别外翻。",
            posture = "仰卧凳上，双手托住哑铃或杠铃于胸前；沿弧线过头下放至上臂接近耳朵，再拉回胸前。",
            errors = "用腰部拱起代偿；下放太深让肩关节压力过大；肘部乱晃。",
        ),
        Movement(
            key = "侧平举",
            keywords = listOf("侧平举", "侧举", "侧 抬举", "侧卧 一 手 抬举"),
            drive = "三角肌中束主导，手肘带着手臂往两侧抬，小指略高于拇指。",
            posture = "站直，肩胛略沉；抬到与肩同高即停，肘微屈固定，回程控制住。",
            errors = "耸肩让斜方肌抢力；用身体前后晃借力；抬过肩太多造成肩峰夹挤。",
        ),
        Movement(
            key = "前平举",
            keywords = listOf("前平举", "前 抬举", "前 肩部抬举"),
            drive = "三角肌前束主导，把重量往正前方抬到肩高。",
            posture = "站直，核心收紧；手臂伸直抬到与肩平，下放时控制住别松掉。",
            errors = "用腰后仰借力；抬得高过肩；下放时完全放掉张力。",
        ),
        Movement(
            key = "反向飞鸟",
            keywords = listOf("反向飞鸟", "俯身飞鸟", "后 三角肌"),
            drive = "三角肌后束主导，手肘往两侧后方打开。",
            posture = "俯身或反向坐姿，胸贴垫、背部挺直；肩胛先收再抬，抬到上臂与躯干同高。",
            errors = "用斜方肌耸肩；身体上下起伏借力；手肘弯成划船。",
        ),
        Movement(
            key = "直立划船",
            keywords = listOf("直立划船", "直立 划船"),
            drive = "三角肌中束配斜方肌上部，手肘领着手往上走。",
            posture = "站直，握距略宽于肩；抬到上臂接近与地面平行即停，不要高过肩。",
            errors = "拉得太高造成肩峰夹挤；手腕先于肘部抬起；用身体上下弹。",
        ),
        Movement(
            key = "面拉",
            keywords = listOf("面拉"),
            exclude = listOf("拉伸"),
            drive = "三角肌后束配菱形肌，把绳子往脸的方向拉并外旋。",
            posture = "滑轮调到面部高度，双手握住绳头；拉到双手在耳侧、肘高于手，肩胛骨后收。",
            errors = "重量过大靠身体后仰；只拉不外旋；手肘掉下去变成划船。",
        ),
        Movement(
            key = "推举",
            keywords = listOf("推举", "肩推"),
            drive = "三角肌前中束主导，把手往头顶「顶」出去，顶端手臂贴耳。",
            posture = "站或坐稳，核心收紧；上臂从肩高推到头顶正上方，肘别往后甩。",
            errors = "塌腰后仰借力；推的路径往前变成斜上推；下放过低让肩关节夹挤。",
        ),
        Movement(
            key = "耸肩",
            keywords = listOf("耸肩"),
            drive = "斜方肌上束，肩胛骨垂直向上提，顶端停一秒。",
            posture = "站直，手臂自然下垂；肩膀垂直往上够耳朵，不要往前转圈。",
            errors = "转肩画圈；用膝盖弹动借力；幅度太小等于没做。",
        ),
        Movement(
            key = "引体",
            keywords = listOf("引体", "双力臂", "身体向上", "悬垂 划船"),
            drive = "背阔肌主导，想象用肘往腰里夹，别只用手臂硬拉。",
            posture = "正握略宽于肩，先沉肩再拉；拉到下巴过杆，下放控制到手臂伸直。",
            errors = "甩腿借力；只做半程；耸肩用斜方肌往上提。",
        ),
        Movement(
            key = "下拉",
            keywords = listOf("下拉"),
            drive = "背阔肌主导，手肘往下后方走，把手柄拉到锁骨位置。",
            posture = "坐稳，大腿卡住压垫，上身微后倾 10–15°；拉到上胸，回程控制到手臂伸直。",
            errors = "身体大幅后仰变成划船；拉到颈后；耸肩。",
        ),
        Movement(
            key = "划船",
            keywords = listOf("划船"),
            drive = "背部主导，先收肩胛再把肘往后拉，胸口挺起。",
            posture = "俯身或坐姿，脊柱保持中立；把重量拉到腹部，手肘贴近身体走。",
            errors = "耸肩只用手臂拉；弓背；身体上下起伏甩重量。",
        ),
        Movement(
            key = "硬拉",
            keywords = listOf("硬拉"),
            drive = "臀与腘绳肌主导，双脚「蹬地」把地推开，杠铃贴着腿走。",
            posture = "杠在脚掌中线正上方，屈髋屈膝下蹲握杠；胸挺背直，起身时髋膝同时伸展，站直即可不要后仰。",
            errors = "弓背起；杠离身体太远；顶端过度后仰压腰。",
            tempo = "每次起杠前先深吸气绷住核心，落地后重新摆好姿势再做下一次。",
        ),
        Movement(
            key = "奥举",
            keywords = listOf("抓举", "挺举", "翻举", "高翻", "翻轮胎", "推蹲"),
            drive = "下肢爆发配髋伸，重量靠腿「送」起来，手臂只负责接住。",
            posture = "杠贴小腿，背部绷紧；先慢起，过膝后加速伸髋，接杠时快速下蹲钻到杠下。",
            errors = "用手臂硬拉；起杠时背弓；接杠时膝盖内扣。",
            tempo = "一次一次做，组间充分休息；每次起杠前深吸气绷住核心。",
        ),
        Movement(
            key = "壶铃摆动",
            keywords = listOf("摆动", "8字", "风车"),
            drive = "髋部爆发伸展把壶铃甩出去，手臂只是绳子。",
            posture = "站距略宽于肩，屈髋把壶铃摆到两腿之间；伸髋站直让壶铃荡到胸口高度。",
            errors = "用手臂抬；做成深蹲；顶端过度后仰。",
        ),
        Movement(
            key = "深蹲",
            keywords = listOf("深蹲", "前蹲"),
            drive = "股四头肌与臀大肌主导，膝盖沿脚尖方向推出去。",
            posture = "脚略宽于肩、脚尖微外八，杠放在上背或前架；下蹲到大腿接近平行或更深，膝盖对准脚尖。",
            errors = "膝盖内扣；脚跟离地；弓腰「早安式」起身。",
        ),
        Movement(
            key = "腿举",
            keywords = listOf("腿举"),
            drive = "股四头肌与臀主导，用整只脚掌把踏板推出去。",
            posture = "背和臀贴紧靠垫，脚与肩同宽踩在踏板中部；下放到膝约 90°，推到接近伸直。",
            errors = "顶端锁死膝盖；下放过深让腰离开靠垫；膝盖内扣。",
        ),
        Movement(
            key = "腿屈伸",
            keywords = listOf("腿屈伸", "腿伸展"),
            exclude = listOf("拉伸"),
            drive = "股四头肌单独发力，顶端停一秒挤紧。",
            posture = "靠背贴紧，膝关节对准器械转轴，踝垫压在脚背上；伸到接近完全伸直。",
            errors = "甩腿利用惯性；抬臀离开座椅；重量大只做半程。",
        ),
        Movement(
            key = "腿弯举",
            keywords = listOf("腿弯举"),
            drive = "腘绳肌主导，脚跟往臀部方向勾。",
            posture = "髋贴紧垫面，膝关节对准转轴；屈膝到接近极限，回程慢慢放。",
            errors = "抬臀借力；回程放太快；重量太大导致幅度变小。",
        ),
        Movement(
            key = "弓步",
            keywords = listOf("弓步", "箭步", "登阶", "前后 步"),
            drive = "前腿的臀与股四头肌主导，重心垂直往下沉。",
            posture = "前后脚分开约一大步，前脚全掌踩实；下蹲到前膝约 90°、后膝接近地面，再蹬起。",
            errors = "前膝内扣或超出脚尖太多；身体前倾；步幅太小。",
        ),
        Movement(
            key = "提踵",
            keywords = listOf("提踵", "抬脚尖", "站姿 小腿"),
            drive = "小腿三头肌，踝关节顶到最高点停一秒。",
            posture = "前脚掌踩在踏板边缘，脚跟悬空；下放到小腿有拉伸感，再顶到最高。",
            errors = "弹动借力；幅度太小；膝盖弯得太多把力卸掉。",
        ),
        Movement(
            key = "臀推",
            keywords = listOf("臀推", "臀桥", "臀冲", "桥式", "髋推", "髋 上提", "髋部上提"),
            drive = "臀大肌主导，靠伸髋把髋顶起来，顶端夹臀一秒。",
            posture = "上背靠在凳上或平躺，脚掌踩实；顶到躯干与大腿成一条线，肋骨别外翻。",
            errors = "用腰过伸代偿；脚尖离地、脚跟抬起；顶端不收臀。",
        ),
        Movement(
            key = "髋外展内收",
            keywords = listOf("外展", "内收", "髋 内旋", "髋 外旋", "穿腿后拉", "打水式", "游泳踢腿"),
            exclude = listOf("拉伸"),
            drive = "臀中肌（外展）或内收肌群（内收）单独发力，身体别跟着倒。",
            posture = "坐或卧稳，骨盆保持中立；开到最大幅度后控制回程。",
            errors = "用身体倾斜代偿；幅度小、速度快；骨盆跟着转。",
        ),
        Movement(
            key = "弯举",
            keywords = listOf("弯举"),
            drive = "肱二头肌主导，肘固定在体侧，靠屈肘把重量卷起来。",
            posture = "站或坐稳，上臂贴住身体不动；卷到前臂接近垂直，顶端挤紧半秒，再慢慢放回。",
            errors = "甩腰借力；手肘往前跑；下放太快丢掉张力。",
        ),
        Movement(
            key = "下压",
            keywords = listOf("下压"),
            drive = "肱三头肌主导，手肘夹紧体侧只做伸肘。",
            posture = "站直身体略前倾，上臂贴住身体；压到手臂完全伸直，顶端挤紧半秒。",
            errors = "手肘往外张开；用体重往下压；回程太高让配重片砸到底。",
        ),
        Movement(
            key = "臂屈伸",
            keywords = listOf("臂屈伸", "碎颅者", "肱三头肌", "后摆", "后驱动", "屈伸"),
            drive = "肱三头肌主导，上臂固定只做伸肘。",
            posture = "上臂保持垂直或贴近头部不动；下放到前臂接近与上臂成直角，再伸到接近伸直。",
            errors = "手肘大幅外张；重量大甩头借力；下放时肩膀跟着动。",
        ),
        Movement(
            key = "挺身",
            keywords = listOf("挺身", "背屈伸", "背伸展", "过度伸展", "髋伸展", "骨盆倾斜"),
            drive = "竖脊肌与臀共同发力，靠髋伸把上身抬平。",
            posture = "髋部贴住垫子，脊柱保持中立；下放到躯干接近垂直，抬到身体成一条线即停。",
            errors = "过度后仰压腰；用惯性上下甩；幅度太小等于没练到。",
        ),
        Movement(
            key = "卷腹",
            keywords = listOf(
                "卷腹", "起坐", "卷身", "V 字", "触踵", "触膝", "触趾",
                "仰卧起坐", "死虫", "腿髋上抬", "踢腿坐",
            ),
            drive = "腹直肌主导，靠脊柱屈曲把肋骨拉向骨盆。",
            posture = "仰卧屈膝，腰背贴住地面；卷到肩胛离地即可，不用整段坐起来。",
            errors = "用手拉头；靠髋屈肌把整个身体抬起来；脖子前伸。",
        ),
        Movement(
            key = "空中蹬车",
            keywords = listOf("空中蹬车", "蹬车"),
            drive = "腹斜肌与腹直肌配合，对侧肘膝靠近时呼气挤紧。",
            posture = "仰卧，腰部贴地；对侧肘与膝相触，另一条腿伸直但不落地。",
            errors = "拉脖子；腰部离地；速度太快失去控制。",
        ),
        Movement(
            key = "平板支撑",
            keywords = listOf(
                "平板支撑", "撑体", "侧桥", "前水平", "人体旗帜", "L 型",
                "等长", "肩 轻拍", "俄式挺身", "倒立", "十字支撑", "马耳他式", "尺蠖",
            ),
            drive = "腹横肌与核心整体收紧，靠稳定撑住而不是靠憋气。",
            posture = "肘在肩正下方，身体从头到脚跟成一条直线；臀部不塌不翘，均匀呼吸。",
            errors = "塌腰；撅臀；憋气硬撑。",
            tempo = "保持 20–60 秒，呼吸均匀不憋气；撑不住就停，别靠塌腰撑着。",
        ),
        Movement(
            key = "举腿",
            keywords = listOf("举腿", "抬腿", "抬举", "悬垂", "腿 抬"),
            drive = "下腹主导，靠骨盆后倾把腿抬起来。",
            posture = "仰卧或悬垂，腰背贴住不拱；抬到腿接近垂直，慢慢放但不完全落回。",
            errors = "用惯性甩腿；腰部离地拱起；下放太快。",
        ),
        Movement(
            key = "早安式",
            keywords = listOf("早安式"),
            drive = "腘绳肌与臀主导，髋往后送，背部角度全程不变。",
            posture = "杠铃放在上背，膝盖微屈；屈髋前倾到躯干接近平行地面，再伸髋站起。",
            errors = "弓背；膝盖完全锁死；重量太大变成深蹲。",
        ),
        Movement(
            key = "旋转",
            keywords = listOf("旋转", "转体", "绕环", "绕臂", "扭转", "环绕", "体侧屈", "侧 屈", "旋前", "旋后"),
            drive = "腹斜肌或前臂旋转肌主导，靠躯干/腕部主动转动，不是甩手臂。",
            posture = "坐或站稳，骨盆保持稳定；转到能控制住的幅度再回正。",
            errors = "只甩手臂不转躯干；速度过快失控；腰部代偿。",
        ),
        Movement(
            key = "健腹轮",
            keywords = listOf("滚轮", "滚动", "身体锯", "健腹轮"),
            drive = "腹部全程绷紧，靠核心拉住身体，别让腰塌下去。",
            posture = "跪姿握轮，从膝盖到头的身体保持一条线；推到能控制的最远处再拉回。",
            errors = "塌腰让腰吃力；推得太远收不回来；用手臂发力拉。",
        ),
        Movement(
            key = "负重行走",
            keywords = listOf("行走", "农夫"),
            drive = "核心与握力为主，肩胛稳定、身体直立往前走。",
            posture = "双手提重物，肩胛下沉，小步快走；躯干不左右侧倾。",
            errors = "耸肩驼背；身体左右晃；步幅过大。",
        ),
        Movement(
            key = "战绳",
            keywords = listOf("战绳", "大锤", "砸"),
            drive = "靠核心与肩带动，快速制造绳波，全程核心收紧。",
            posture = "半蹲站稳，膝微屈，躯干稳定；手臂做快速上下交替。",
            errors = "全靠手臂甩；塌腰；憋气。",
            tempo = "20–30 秒一组，保持呼吸节奏，宁可慢一点也别乱甩。",
        ),
        Movement(
            key = "药球",
            keywords = listOf("药球"),
            drive = "靠下肢与核心的爆发传到球上，手臂只是最后一环。",
            posture = "站稳、核心收紧，投或砸时全身一起发力，出手后收住身体。",
            errors = "只用上肢甩；腰过度反弓；接球时不缓冲。",
        ),
        Movement(
            key = "拉伸",
            keywords = listOf("拉伸", "伸展", "瑜伽", "狮身", "犬式", "针引线", "抱膝", "贴胸", "广角", "坐角"),
            drive = "拉到肌肉有轻微牵拉感即可，不追求疼痛。",
            posture = "摆好姿势后缓慢进入，让被拉伸的肌肉放松；保持 20–30 秒，期间正常呼吸。",
            errors = "弹震式猛拉；憋气；拉到刺痛还硬撑。",
            tempo = "静态保持 20–30 秒，呼气时稍微加深一点，不要憋气。",
        ),
        Movement(
            key = "有氧",
            keywords = listOf(
                "步行", "跑", "骑车", "单车", "爬楼", "椭圆", "跳绳", "开合",
                "波比", "登山者", "熊爬", "爬绳", "滑冰", "剪刀", "分腿跳",
                "星形", "高抬膝", "手摇车", "滑雪", "跳箱",
            ),
            drive = "保持稳定节奏，能说短句但不会喘到说不出话。",
            posture = "身体直立，目视前方，脚掌落在身体正下方；手臂自然摆动。",
            errors = "身体趴在扶手上；跨步过大；一开始就冲太快。",
            tempo = "先慢速热身 3–5 分钟再进入目标强度，结束前慢下来 2 分钟降心率。",
        ),
    )

    /** 按名字识别动作模式；识别不出时返回 null，由调用方退化到部位级兜底。 */
    fun movementOf(name: String): Movement? {
        val compact = name.replace(" ", "")
        return movements.firstOrNull { movement ->
            val excluded = movement.exclude.any { it.replace(" ", "") in compact }
            !excluded && movement.keywords.any { it.replace(" ", "") in compact }
        }
    }

    /**
     * 器械相关的通用提示，接在「姿态与幅度」后面。
     *
     * 只写器械特有的设定与风险，动作本身的技术要点在上面的模式文案里，
     * 避免同一个动作两处说同一件事。
     */
    fun equipmentNote(equipmentDetail: String?): String? {
        return when (equipmentDetail?.trim()?.lowercase()) {
            "cable" -> "先把滑轮高度和站位调好，回程别让配重片砸到底。"
            "leverage machine", "smith machine", "sled machine" ->
                "先调座椅，让关节对准器械转轴，别靠惯性甩。"
            "band", "resistance band" -> "固定点要牢，回程控制住，别让弹力带把手弹回去。"
            "barbell", "dumbbell", "ez barbell", "olympic barbell", "trap bar", "kettlebell" ->
                "重量以能控制全程为准，动作一变形就减重量。"
            else -> null
        }
    }

    /** 器械相关的典型错误，接在「常见错误」后面。 */
    fun equipmentError(equipmentDetail: String?): String? {
        return when (equipmentDetail?.trim()?.lowercase()) {
            "cable" -> "回程时被配重片拽回去。"
            "leverage machine", "smith machine", "sled machine" -> "把固定轨迹当保险，用惯性甩起来。"
            "band", "resistance band" -> "起始位置没拉紧，动作前半程是空的。"
            else -> null
        }
    }

    /**
     * 识别不出动作模式时的兜底：按部位给一句通用要点。
     *
     * 这类动作（约 4%）名字过于笼统，硬编细节等于编造，只给「怎么发力、注意什么」的通用话术。
     */
    fun bodyFallback(bodyCategoryId: String?): Pair<String, String>? {
        return when (bodyCategoryId) {
            ExerciseClassification.CHEST.id ->
                "胸大肌主导" to "肩胛骨稳住不耸肩，下放和推起都在控制之内，别靠甩。"
            ExerciseClassification.BACK.id ->
                "背部主导" to "先沉肩收肩胛再发力，别只用手臂拉。"
            ExerciseClassification.SHOULDERS.id ->
                "三角肌主导" to "肩胛稳定，抬到与肩平即可，不追求过顶幅度。"
            ExerciseClassification.UPPER_ARMS.id ->
                "上臂肌群主导" to "手肘位置固定，只做单关节的屈或伸。"
            ExerciseClassification.LOWER_ARMS.id ->
                "前臂主导" to "前臂贴住支撑点，只做腕部的屈伸或旋转。"
            ExerciseClassification.UPPER_LEGS.id ->
                "臀腿主导" to "膝盖沿脚尖方向走，脚掌踩实，重心别跑偏。"
            ExerciseClassification.LOWER_LEGS.id ->
                "小腿主导" to "踝关节做满幅度，顶端停一下再放。"
            ExerciseClassification.WAIST.id ->
                "核心主导" to "腰背贴紧、肋骨下沉，靠腹部收缩带动。"
            ExerciseClassification.CARDIO.id ->
                "心肺为主" to "保持能持续下去的节奏，呼吸均匀不憋气。"
            else -> null
        }
    }
}
