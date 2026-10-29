# Reescrita dos plugins

O planejamento é uma suíte Gradle com um jar por módulo. O mapa abaixo preserva a decisão de reescrever os plugins em Java; os nomes indicam o escopo futuro, não funcionalidades já disponíveis.

| Módulo | Situação | Originais ou responsabilidade |
| --- | --- | --- |
| gold-api | Base inicial | Interfaces dos serviços comuns |
| GoldCore | Parcial | SQLite assíncrono e HMAC prontos; menus, hologramas, cooldowns e eventos pendentes; aFloatingText |
| GoldAuth | Inicial implementado e testado com jogadores simulados | ServerAuth, ChatLogin, OpLogin; falta validar cliente real e ampliar configuração |
| GoldPerms | Pendente | PurePerms |
| GoldVip | Pendente | Key, Vips, VipJoin |
| GoldEconomy | Pendente | EconomyAPI, Cash, TCashh, lxp, VenderAll, EconomySellPro, TopMoney |
| GoldShop | Pendente | EconomyShop, EconomyPShop, EconomyCShop, ShopI |
| GoldMarket | Pendente | AnuncioByReds; leilão no lugar de /olx |
| GoldEssentials | Pendente | BasicHomes, essentialsTP, Back, Fly, TPALL, MSpawns, RespawnLobby, ArenaFactions, TpFavela, Vip_and_Loja, IpGold |
| GoldChat | Pendente | PureChat, AntiSpam, DesabilitaChat, Limpar_Chat, DesativarTell, TellCustom, AutoMensagem, CustomAlerts, Bloozt_TIP |
| GoldProtect | Pendente | WorldProtect/ForceFieldSnipTM/Proteção, iProtector, AreaEfeitos, Ant-Void, anti-XRay |
| GoldFactions | Pendente | FactionsPro, TopFacs |
| GoldPvP | Pendente | CombateL, Perfil, QuebrarArmadura, Creeper |
| GoldCrates | Pendente | CaixaS, ImpleCaixaS |
| GoldKits | Pendente | AdvancedKits |
| GoldVault | Pendente | EnderChest |
| GoldBreaker | Pendente | BedrockBreaker, ObsidianBreaker, endbreaker, BedRockBreak, /reparar, /enviar |
| GoldEnchant | Pendente | Encantar, Mago |
| GoldWorld | Pendente | ManyWorlds, AlphaVille, RealTime, MegaFornalha, SilkSpawners, StackingMobs, AutoInv |
| GoldBuild | Pendente | MagicWE refeito do zero, BlockSniper, Montanhas |
| GoldNPC | Pendente | Slapper e SlapperRotation |
| GoldHud | Pendente | superBAR, Cordenadas, itemID, Status Armor |
| GoldStaff | Pendente | Report, ajuda, GiveAllPN, CommandBlocker, Restart, ClearLagg, TimeCommander |

DevTools fica fora da suíte por ser ferramenta de empacotamento PHP. GoldScoreboard, GoldDaily, GoldAntiCheat e GoldMenu permanecem opcionais para uma segunda fase.
