param(
    [string]$Raiz = (Split-Path -Parent $PSScriptRoot),
    [string]$Nome = 'integracao',
    [switch]$Bash,
    [switch]$SemArgumento,
    [switch]$Direto = $true,
    [string]$PluginJar = (Join-Path $Raiz 'build/gold-core-regression.jar'),
    [switch]$FalhasEsperadas = $true,
    [string[]]$OutrosPlugins = @()
)
$ErrorActionPreference = 'Stop'
$pastaBuild = [IO.Path]::GetFullPath((Join-Path $Raiz 'build'))
$teste = [IO.Path]::GetFullPath((Join-Path $pastaBuild $Nome))
if (!$teste.StartsWith($pastaBuild + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'A pasta de testes precisa ficar dentro de build/.' }
New-Item -ItemType Directory -Path $teste -Force | Out-Null
Copy-Item (Join-Path $Raiz 'start.bat') (Join-Path $teste 'start.bat') -Force
Copy-Item (Join-Path $Raiz 'start.sh') (Join-Path $teste 'start.sh') -Force
if ($PluginJar) {
    New-Item -ItemType Directory -Force -Path (Join-Path $teste 'plugins') | Out-Null
    Copy-Item -LiteralPath $PluginJar -Destination (Join-Path $teste 'plugins/regression.jar') -Force
    foreach ($outro in $OutrosPlugins) { Copy-Item -LiteralPath $outro -Destination (Join-Path $teste 'plugins') -Force }
}
$jar = Join-Path $Raiz 'build/nukkit-gold.jar'
$configuracao = [System.Diagnostics.ProcessStartInfo]::new()
$configuracao.FileName = 'cmd.exe'
$configuracao.Arguments = '/d /c ""' + (Join-Path $teste 'start.bat') + '" "' + $jar + '""'
if ($SemArgumento) {
    Copy-Item $jar (Join-Path $teste 'nukkit-gold.jar') -Force
    $configuracao.Arguments = '/d /c ""' + (Join-Path $teste 'start.bat') + '""'
}
if ($Bash) {
    $configuracao.FileName = 'C:\Users\gusta\AppData\Local\Programs\Git\bin\bash.exe'
    $configuracao.Arguments = '"' + (Join-Path $teste 'start.sh').Replace('\', '/') + '" "' + $jar.Replace('\', '/') + '"'
}
$configuracao.WorkingDirectory = $PSScriptRoot
if ($Direto) {
    $configuracao.FileName = (Get-Command java.exe).Source
    $configuracao.Arguments = '-Xmx1G -Dfile.encoding=windows-1252 -jar "' + $jar + '"'
    $configuracao.WorkingDirectory = $teste
}
$configuracao.UseShellExecute = $false
$configuracao.CreateNoWindow = $true
$configuracao.RedirectStandardInput = $true
$configuracao.RedirectStandardOutput = $true
$configuracao.RedirectStandardError = $true
$configuracao.StandardOutputEncoding = [System.Text.Encoding]::UTF8
$configuracao.StandardErrorEncoding = [System.Text.Encoding]::UTF8
$processo = [System.Diagnostics.Process]::new()
$processo.StartInfo = $configuracao
$null = $processo.Start()
$saida = [System.Text.StringBuilder]::new()
$erros = [System.Text.StringBuilder]::new()
$bufferSaida = [char[]]::new(4096)
$bufferErros = [char[]]::new(4096)
$leituraSaida = $processo.StandardOutput.ReadAsync($bufferSaida, 0, $bufferSaida.Length)
$leituraErros = $processo.StandardError.ReadAsync($bufferErros, 0, $bufferErros.Length)
$prazo = [DateTime]::UtcNow.AddSeconds(60)
$idiomaEnviado = $false
$paradaEnviada = $false
$finalizado = $false
$saidaEncerrada = $false
$errosEncerrados = $false
try {
    while ([DateTime]::UtcNow -lt $prazo) {
        if (!$saidaEncerrada -and $leituraSaida.IsCompleted) {
            $quantidade = $leituraSaida.GetAwaiter().GetResult()
            if ($quantidade -gt 0) {
                $trecho = [string]::new($bufferSaida, 0, $quantidade)
                $null = $saida.Append($trecho)
                Write-Host -NoNewline $trecho
                $leituraSaida = $processo.StandardOutput.ReadAsync($bufferSaida, 0, $bufferSaida.Length)
            } else { $saidaEncerrada = $true }
        }
        if (!$errosEncerrados -and $leituraErros.IsCompleted) {
            $quantidade = $leituraErros.GetAwaiter().GetResult()
            if ($quantidade -gt 0) {
                $trecho = [string]::new($bufferErros, 0, $quantidade)
                $null = $erros.Append($trecho)
                Write-Host -NoNewline $trecho
                $leituraErros = $processo.StandardError.ReadAsync($bufferErros, 0, $bufferErros.Length)
            } else { $errosEncerrados = $true }
        }
        if (!$idiomaEnviado -and $saida.ToString().Contains('Please choose a language')) {
            $processo.StandardInput.WriteLine('bra')
            $processo.StandardInput.Flush()
            $idiomaEnviado = $true
        }
        if (!$paradaEnviada -and $saida.ToString().Contains('Finalizado') -and (!$PluginJar -or $saida.ToString().Contains('VERIFICACOES_INTEGRADAS_APROVADAS'))) {
            $finalizado = $true
            Start-Sleep -Milliseconds 1000
            Get-NetUDPEndpoint -LocalPort 19132 -ErrorAction SilentlyContinue | Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table | Out-String | Write-Output
            # Confere o ping RakNet antes de desligar o processo de teste.
            $udp = [System.Net.Sockets.UdpClient]::new()
            try {
                $udp.Client.ReceiveTimeout = 1000
                $udp.Connect('127.0.0.1', 19132)
                [byte[]]$ping = 0x01,0,0,0,0,0,0,0,1,0,0xff,0xff,0,0xfe,0xfe,0xfe,0xfe,0xfd,0xfd,0xfd,0xfd,0x12,0x34,0x56,0x78,0,0,0,0,0,0,0,1
                $origem = [System.Net.IPEndPoint]::new([System.Net.IPAddress]::Any, 0)
                $resposta = $null
                for ($tentativa = 0; $tentativa -lt 5; $tentativa++) {
                    $null = $udp.Send($ping, $ping.Length)
                    try { $resposta = $udp.Receive([ref]$origem); break } catch { Start-Sleep -Milliseconds 300 }
                }
                if ($null -eq $resposta) { throw 'Ping sem resposta após cinco tentativas.' }
                $textoPing = [System.Text.Encoding]::UTF8.GetString($resposta, 35, $resposta.Length - 35)
                Write-Output "Ping RakNet: $textoPing"
                if ($resposta[0] -ne 0x1c -or !$textoPing.Contains(';84;0.15.10;')) { throw 'Resposta de ping inesperada.' }
                $textoPing | Set-Content (Join-Path $teste 'ping.txt') -Encoding utf8NoBOM
            } finally { $udp.Dispose() }
            if ($PluginJar) { $processo.StandardInput.WriteLine('verificar_thread_gold') }
            # Com barra, como se digita no jogo; o console precisa aceitar.
            $processo.StandardInput.WriteLine('/list')
            $processo.StandardInput.WriteLine('stop')
            $processo.StandardInput.Flush()
            $paradaEnviada = $true
        }
        if ($processo.HasExited -and $saidaEncerrada -and $errosEncerrados) { break }
        Start-Sleep -Milliseconds 50
    }
    if (!$processo.HasExited) { throw 'Servidor não encerrou dentro do prazo de 60 segundos.' }
    $processo.WaitForExit()
    if (!$finalizado) { throw 'Servidor não chegou a Finalizado.' }
    if ($processo.ExitCode -ne 0) { throw "Código de saída inesperado: $($processo.ExitCode)" }
    $diagnosticos = $saida.ToString() + $erros.ToString()
    if ($diagnosticos.Contains('SEGREDO_NAO_DEVE_APARECER')) { throw 'Argumentos sensiveis foram registrados no console.' }
    foreach ($senhaTeste in @('SenhaTesteSegura123', 'NovaSenhaSegura456', 'SenhaIncorreta123', 'SenhaSempreErrada123', 'NaoSobrescrever123')) {
        if ($diagnosticos.Contains($senhaTeste)) { throw 'Senha de teste apareceu no console.' }
    }
    if ($FalhasEsperadas) {
        $inicioFalhas = $diagnosticos.IndexOf('INICIO_FALHAS_ESPERADAS')
        $fimFalhas = $diagnosticos.IndexOf('FIM_FALHAS_ESPERADAS')
        if ($inicioFalhas -lt 0 -or $fimFalhas -le $inicioFalhas) { throw 'Faltam delimitadores das falhas simuladas.' }
        $diagnosticos = $diagnosticos.Remove($inicioFalhas, $fimFalhas - $inicioFalhas)
    }
    if ($diagnosticos -match '(?i)exception|\[ERROR\]|\[ALERT\]|\[CRITICAL\]|UnsatisfiedLinkError|Failed to (load|create)|Unable to start') { throw 'Erro detectado na saída do servidor.' }
    if (!$saida.ToString().Contains('Parando o servidor')) { throw 'Comando stop não confirmado.' }
    if (!$saida.ToString().Contains('jogadores(as) online') -or $saida.ToString().Contains('Comando desconhecido')) { throw 'Comando /list do console não foi aceito.' }
    Write-Output 'Teste aprovado: Finalizado, ping protocolo 84, comandos e encerramento sem falhas inesperadas.'
} finally {
    $saida.ToString() | Set-Content (Join-Path $teste 'console-stdout.txt') -Encoding utf8NoBOM
    $erros.ToString() | Set-Content (Join-Path $teste 'console-stderr.txt') -Encoding utf8NoBOM
    if (!$processo.HasExited) { $processo.Kill($true) }
    $processo.Dispose()
}
