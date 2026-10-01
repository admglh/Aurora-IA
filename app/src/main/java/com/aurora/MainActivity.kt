package com.aurora

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurora.core.database.ConversationEntity
import com.aurora.core.database.MemoryEntity
import com.aurora.core.database.Roles
import com.aurora.core.model.ModelState
import com.aurora.ui.AuroraViewModel

private val Black = Color(0xFF02050A)
private val Ink = Color(0xFF07101A)
private val Panel = Color(0xFF0B1521)
private val Panel2 = Color(0xFF101E2E)
private val Line = Color(0xFF1B3046)
private val Blue = Color(0xFF36A9FF)
private val Cyan = Color(0xFF76E3FF)
private val Purple = Color(0xFF9C8CFF)
private val White = Color(0xFFF1F7FF)
private val Muted = Color(0xFF8495AA)
private val User = Color(0xFF0B344A)
private val Amber = Color(0xFFF2C35F)
private val Red = Color(0xFFF27D7D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Purple, background = Black, surface = Panel, onSurface = White)) {
                AuroraApp()
            }
        }
    }
}

private enum class Screen { HOME, CHAT, TOOLS, MEMORY, SETTINGS }

@Composable
private fun AuroraApp(vm: AuroraViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val modelState by vm.modelState.collectAsState()
    val generating by vm.isGenerating.collectAsState()
    val error by vm.error.collectAsState()
    val conversations by vm.conversations.collectAsState()
    val memories by vm.memories.collectAsState()
    val activeId by vm.activeConversationId.collectAsState()
    val animationEnabled by vm.animationEnabled.collectAsState()
    val voiceEnabled by vm.voiceEnabled.collectAsState()
    val voicePitch by vm.voicePitch.collectAsState()
    val voiceRate by vm.voiceRate.collectAsState()
    val localModelName by vm.localModelName.collectAsState()
    val deviceInfo by vm.deviceInfo.collectAsState()

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var input by rememberSaveable { mutableStateOf("") }
    var showChats by remember { mutableStateOf(false) }
    var showModel by remember { mutableStateOf(false) }
    var memoryToDelete by remember { mutableStateOf<MemoryEntity?>(null) }
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let(vm::importModel) }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty() && screen == Screen.CHAT) listState.scrollToItem(messages.lastIndex)
    }

    Scaffold(containerColor = Black) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    modelState = modelState,
                    localModelName = localModelName,
                    animationEnabled = animationEnabled,
                    onOpen = { screen = it },
                    onNewChat = { vm.newConversation(); screen = Screen.CHAT }
                )
                Screen.CHAT -> ChatScreen(
                    messages = messages,
                    modelState = modelState,
                    generating = generating,
                    input = input,
                    onInput = { input = it },
                    onSend = { if (vm.send(input)) input = "" },
                    onStop = vm::stop,
                    onSpeak = vm::speakLast,
                    onChats = { showChats = true },
                    onBack = { screen = Screen.HOME },
                    listState = listState,
                    error = error,
                    onDismissError = vm::dismissError
                )
                Screen.TOOLS -> ToolsScreen(deviceInfo, onBack = { screen = Screen.HOME })
                Screen.MEMORY -> MemoryScreen(memories, onBack = { screen = Screen.HOME }, onDelete = { memoryToDelete = it })
                Screen.SETTINGS -> SettingsScreen(
                    animationEnabled, voiceEnabled, voicePitch, voiceRate, localModelName,
                    vm.remoteEndpoint(), vm.remoteModel(), vm.hasRemoteKey(),
                    onBack = { screen = Screen.HOME },
                    onAnimation = vm::setAnimationEnabled,
                    onVoice = vm::setVoiceEnabled,
                    onPitch = vm::setVoicePitch,
                    onRate = vm::setVoiceRate,
                    onModel = { showModel = true },
                    onImport = { picker.launch(arrayOf("application/octet-stream", "application/*")) },
                    onSaveRemote = vm::saveRemoteConfig,
                    onClearRemote = vm::clearRemoteConfig,
                    onNew = vm::newConversation
                )
            }
        }
    }

    if (showChats) {
        ConversationsDialog(conversations, activeId, vm::openConversation, vm::deleteConversation) { showChats = false }
    }
    if (showModel) {
        ModelDialog(
            state = modelState,
            endpoint = vm.remoteEndpoint(),
            model = vm.remoteModel(),
            hasKey = vm.hasRemoteKey(),
            localName = localModelName,
            onSave = { e, m, k -> vm.saveRemoteConfig(e, m, k); showModel = false },
            onClear = { vm.clearRemoteConfig(); showModel = false },
            onImport = { picker.launch(arrayOf("application/octet-stream", "application/*")) },
            onDismiss = { showModel = false }
        )
    }
    memoryToDelete?.let { memory ->
        AlertDialog(
            onDismissRequest = { memoryToDelete = null },
            title = { Text("Apagar memória?") },
            text = { Text(memory.content, color = Muted) },
            confirmButton = { TextButton(onClick = { vm.deleteMemory(memory.id); memoryToDelete = null }) { Text("APAGAR", color = Red) } },
            dismissButton = { TextButton(onClick = { memoryToDelete = null }) { Text("CANCELAR") } }
        )
    }
}

@Composable
private fun HomeScreen(
    modelState: ModelState,
    localModelName: String,
    animationEnabled: Boolean,
    onOpen: (Screen) -> Unit,
    onNewChat: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF030913), Color(0xFF07111F), Black)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "AURORA",
                    color = White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "assistente pessoal",
                    color = Muted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            StatePill(modelState)
        }

        Spacer(Modifier.height(4.dp))

        if (animationEnabled) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                AuroraAnimatedPresence(Modifier.fillMaxSize().alpha(0.58f))
            }
        } else {
            Spacer(Modifier.height(28.dp))
        }

        Text(
            "Aurora",
            color = White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Estou aqui. Escolha o que você quer fazer.",
            color = Muted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeAction(
                "CONVERSAR",
                "Fale com a Aurora",
                "◉",
                { onOpen(Screen.CHAT) },
                Modifier.weight(1f)
            )
            HomeAction(
                "FERRAMENTAS",
                "Ações e recursos",
                "◇",
                { onOpen(Screen.TOOLS) },
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeAction(
                "MEMÓRIA",
                "O que está guardado",
                "⌁",
                { onOpen(Screen.MEMORY) },
                Modifier.weight(1f)
            )
            HomeAction(
                "CONFIGURAÇÕES",
                "Controle a Aurora",
                "⚙",
                { onOpen(Screen.SETTINGS) },
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        SmallButton(
            if (modelState is ModelState.Ready) "NOVA CONVERSA"
            else "CONFIGURAR UM MODELO",
            {
                if (modelState is ModelState.Ready) onNewChat()
                else onOpen(Screen.SETTINGS)
            }
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (localModelName.isBlank())
                "Nenhum modelo local selecionado"
            else
                "Modelo local: $localModelName",
            color = Muted,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AuroraAnimatedPresence(modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                contentDescription = "Aurora"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        context.resources,
                        R.drawable.aurora_animation
                    )
                    val drawable = ImageDecoder.decodeDrawable(source)
                    setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        drawable.start()
                    }
                }
            }
        },
        update = { imageView ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (imageView.drawable as? AnimatedImageDrawable)?.let {
                    if (!it.isRunning) it.start()
                }
            }
        }
    )
}

@Composable
private fun HomeAction(
    title: String,
    subtitle: String,
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(22.dp)),
        color = Panel,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                glyph,
                color = Cyan,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(7.dp))
            Text(
                title,
                color = White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                subtitle,
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2
            )
        }
    }
}

@Composable private fun ChatScreen(messages: List<com.aurora.ui.UiMessage>, modelState: ModelState, generating: Boolean, input: String, onInput: (String) -> Unit, onSend: () -> Unit, onStop: () -> Unit, onSpeak: () -> Unit, onChats: () -> Unit, onBack: () -> Unit, listState: androidx.compose.foundation.lazy.LazyListState, error: String?, onDismissError: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("CONVERSAR", modelLabel(modelState), onBack) { onChats() }
        error?.let { Surface(Modifier.fillMaxWidth().padding(10.dp), color = Color(0xFF35171B), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, Modifier.weight(1f), color = Red); TextButton(onClick = onDismissError) { Text("OK") } } } }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState, contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages, key = { it.id }) { m -> MessageBubble(m.role == Roles.USER, m.text, m.streaming) }
            if (messages.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Converse com a Aurora", color=White, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text("A conversa funciona pelo próprio aplicativo.", color=Muted, textAlign=TextAlign.Center); Spacer(Modifier.height(16.dp)); Text("Configure um modelo em Configurações se ainda não houver um pronto.", color=Muted, textAlign=TextAlign.Center) } }
        }
        Composer(input, onInput, modelState is ModelState.Ready, generating, onSend, onStop, onSpeak)
    }
}

@Composable private fun ToolsScreen(deviceInfo: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("FERRAMENTAS", "Recursos da Aurora", onBack)
        Column(Modifier.padding(18.dp)) {
            ToolCard("Informações do dispositivo", "Consultar fabricante, modelo e versão do Android.", deviceInfo)
            ToolCard("Ferramentas futuras", "A arquitetura está preparada para novos módulos sem transformar a tela inicial em um painel técnico.", "Preparado")
        }
    }
}

@Composable private fun ToolCard(title:String, subtitle:String, value:String) {
    Surface(Modifier.fillMaxWidth().padding(bottom=12.dp).border(1.dp,Line,RoundedCornerShape(22.dp)), color=Panel, shape=RoundedCornerShape(22.dp)) { Column(Modifier.padding(17.dp)) { Text(title,color=White,fontWeight=FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(subtitle,color=Muted); Spacer(Modifier.height(12.dp)); Text(value,color=Cyan,style=MaterialTheme.typography.bodyMedium) } }
}

@Composable private fun MemoryScreen(memories: List<MemoryEntity>, onBack:()->Unit, onDelete:(MemoryEntity)->Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("MEMÓRIA", "Armazenamento local", onBack)
        if (memories.isEmpty()) Text("Nenhuma memória armazenada.", color=Muted, modifier=Modifier.padding(22.dp))
        else LazyColumn(contentPadding=PaddingValues(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) { items(memories, key={it.id}) { m -> Surface(Modifier.fillMaxWidth().border(1.dp,Line,RoundedCornerShape(20.dp)),color=Panel,shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.Top){Column(Modifier.weight(1f)){Text(m.kind.uppercase(),color=Cyan,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(5.dp));Text(m.content,color=White);Text(m.source,color=Muted,style=MaterialTheme.typography.labelSmall)};TextButton(onClick={onDelete(m)}){Text("APAGAR",color=Red)}}} } }
    }
}

@Composable private fun SettingsScreen(animationEnabled:Boolean,voiceEnabled:Boolean,voicePitch:Float,voiceRate:Float,localModelName:String,remoteEndpoint:String,remoteModel:String,hasKey:Boolean,onBack:()->Unit,onAnimation:(Boolean)->Unit,onVoice:(Boolean)->Unit,onPitch:(Float)->Unit,onRate:(Float)->Unit,onModel:()->Unit,onImport:()->Unit,onSaveRemote:(String,String,String)->Unit,onClearRemote:()->Unit,onNew:()->Unit){ Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { TopBar("CONFIGURAÇÕES","Tudo dentro do aplicativo",onBack); Column(Modifier.padding(18.dp)){ SettingCard("PRESENÇA DA AURORA") { ToggleRow("Animação na tela inicial",animationEnabled,onAnimation) }; SettingCard("VOZ DA AURORA") { ToggleRow("Falar respostas",voiceEnabled,onVoice); Text("Graveza",color=Muted); Slider(value=voicePitch,onValueChange=onPitch,valueRange=.55f..1f); Text("Velocidade",color=Muted); Slider(value=voiceRate,onValueChange=onRate,valueRange=.70f..1.05f); Text("A voz é aplicada pelo mecanismo TTS do Android.",color=Muted,style=MaterialTheme.typography.labelSmall) }; SettingCard("MODELO LOCAL") { Text(if(localModelName.isBlank()) "Nenhum GGUF importado" else localModelName,color=White); Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){SmallButton("IMPORTAR GGUF",onImport,Modifier.weight(1f));SmallButton("DETALHES",onModel,Modifier.weight(1f))} }; SettingCard("ENDPOINT OPCIONAL") { Text(if(remoteEndpoint.isBlank()) "Não configurado" else remoteEndpoint,color=White); Text(if(remoteModel.isBlank()) "Nenhum modelo" else remoteModel,color=Muted); Text(if(hasKey) "Chave armazenada com segurança" else "Sem chave",color=Muted);Spacer(Modifier.height(8.dp));SmallButton("CONFIGURAR",onModel) }; SettingCard("CONVERSAS") { Text("O histórico fica no armazenamento local.",color=Muted);Spacer(Modifier.height(8.dp));SmallButton("NOVA CONVERSA",onNew) }; Spacer(Modifier.height(28.dp)) } } }

@Composable private fun TopBar(title:String,subtitle:String,onBack:()->Unit,onAction:(()->Unit)?=null){ Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal=10.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Text("‹",color=Cyan,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.clickable(onClick=onBack).padding(horizontal=8.dp));Column(Modifier.weight(1f)){Text(title,color=White,fontWeight=FontWeight.Bold);Text(subtitle,color=Muted,style=MaterialTheme.typography.labelSmall)};onAction?.let{Text("☰",color=Cyan,style=MaterialTheme.typography.titleLarge,modifier=Modifier.clickable{it()}.padding(8.dp))}} }
@Composable private fun StatePill(state:ModelState){val (t,c)=when(state){is ModelState.Ready->"● CONECTADA" to Cyan;is ModelState.Generating->"● GERANDO" to Purple;is ModelState.Loading->"● VERIFICANDO" to Muted;is ModelState.NotConfigured->"● SEM MODELO" to Amber;is ModelState.Error->"● ERRO" to Red};Surface(color=c.copy(alpha=.10f),shape=RoundedCornerShape(50.dp),border=androidx.compose.foundation.BorderStroke(1.dp,c.copy(alpha=.35f))){Text(t,color=c,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))}}
@Composable private fun MessageBubble(user:Boolean,text:String,streaming:Boolean){Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user) Arrangement.End else Arrangement.Start){Surface(Modifier.fillMaxWidth(if(user).88f else .94f),color=if(user) User else Panel,shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,if(user) Color(0xFF195777) else Line)){Column(Modifier.padding(15.dp)){Text(if(user)"VOCÊ" else "AURORA",color=if(user)Cyan else Purple,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text(if(streaming&&text.isBlank())"…" else text,color=White,style=MaterialTheme.typography.bodyLarge)}}}}
@Composable private fun Composer(value:String,onValue:(String)->Unit,ready:Boolean,generating:Boolean,onSend:()->Unit,onStop:()->Unit,onSpeak:()->Unit){Surface(Modifier.fillMaxWidth().navigationBarsPadding(),color=Ink){Column(Modifier.padding(10.dp)){Row(verticalAlignment=Alignment.Bottom){OutlinedTextField(value,onValue,Modifier.weight(1f).heightIn(min=52.dp,max=140.dp),placeholder={Text(if(ready)"Escreva para a Aurora…" else "Escreva uma mensagem…")},enabled=!generating,shape=RoundedCornerShape(18.dp));Spacer(Modifier.width(7.dp));Button(onClick=if(generating)onStop else onSend,enabled=inputActionEnabled(ready, value, generating),shape=CircleShape,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(52.dp)){Text(if(generating)"■" else "➤")}};if(ready)TextButton(onClick=onSpeak,modifier=Modifier.align(Alignment.End)){Text("OUVIR ÚLTIMA RESPOSTA",color=Muted,style=MaterialTheme.typography.labelSmall)}}}}
@Composable private fun ConversationsDialog(items:List<ConversationEntity>,active:Long?,onOpen:(Long)->Unit,onDelete:(Long)->Unit,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("CONVERSAS")},text={LazyColumn{items(items){c->Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text(c.title,Modifier.weight(1f).clickable{onOpen(c.id)},color=if(c.id==active)Cyan else White,maxLines=2,overflow=TextOverflow.Ellipsis);TextButton(onClick={onDelete(c.id)}){Text("APAGAR",color=Red)}}}}},confirmButton={TextButton(onClick=onDismiss){Text("FECHAR")}})}
@Composable private fun ModelDialog(state:ModelState,endpoint:String,model:String,hasKey:Boolean,localName:String,onSave:(String,String,String)->Unit,onClear:()->Unit,onImport:()->Unit,onDismiss:()->Unit){var e by remember(endpoint){mutableStateOf(endpoint)};var m by remember(model){mutableStateOf(model)};var k by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("MODELO")},text={Column(Modifier.verticalScroll(rememberScrollState())){Text(stateDescription(state),color=Muted);Spacer(Modifier.height(12.dp));Text("GGUF local",color=Cyan,fontWeight=FontWeight.Bold);Text(if(localName.isBlank())"Nenhum modelo importado" else localName,color=White);Spacer(Modifier.height(8.dp));SmallButton("IMPORTAR MODELO",onImport);Spacer(Modifier.height(14.dp));HorizontalDivider(color=Line);Spacer(Modifier.height(14.dp));Text("Endpoint compatível com OpenAI",color=Cyan,fontWeight=FontWeight.Bold);OutlinedTextField(e,{e=it},label={Text("Endpoint /v1")});OutlinedTextField(m,{m=it},label={Text("Modelo")});OutlinedTextField(k,{k=it},label={Text(if(hasKey)"Nova chave (opcional)" else "Chave (opcional)")})}},confirmButton={TextButton(onClick={onSave(e,m,k)}){Text("SALVAR")}},dismissButton={TextButton(onClick=onClear){Text("LIMPAR")}})}
@Composable private fun SettingCard(title: String, content: @Composable () -> Unit){Surface(Modifier.fillMaxWidth().padding(bottom=12.dp).border(1.dp,Line,RoundedCornerShape(22.dp)),color=Panel,shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(16.dp)){Text(title,color=Cyan,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));content()}}}
@Composable private fun ToggleRow(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),color=White);Switch(checked=value,onCheckedChange=onChange)}}
@Composable private fun BigButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier){Button(onClick=onClick,modifier=modifier.height(54.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF0A4772))){Text(text,fontWeight=FontWeight.Bold)}}
@Composable private fun SmallButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier){Button(onClick,modifier.height(44.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Panel2),border=androidx.compose.foundation.BorderStroke(1.dp,Line)){Text(text,color=White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium)}}
private fun inputActionEnabled(ready: Boolean, value: String, generating: Boolean): Boolean =
    generating || value.trim().isNotEmpty()

private fun modelLabel(s:ModelState)=when(s){is ModelState.Ready->s.label;is ModelState.NotConfigured->"Nenhum modelo configurado";is ModelState.Loading->s.detail;is ModelState.Generating->"Aurora está respondendo…";is ModelState.Error->"Erro: ${s.message}"}
private fun stateDescription(s:ModelState)=when(s){is ModelState.Ready->"Conectada: ${s.label}";is ModelState.NotConfigured->s.reason;is ModelState.Loading->s.detail;is ModelState.Generating->"Aurora está gerando uma resposta.";is ModelState.Error->s.message}
