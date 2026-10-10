# [B17] 空槽检测（unless + *） | 期望匹配
scoreboard objectives add tt dummy
clear @s
execute unless items entity @s hotbar.5 * run scoreboard players set #b17 tt 1
execute if items entity @s hotbar.5 * run scoreboard players set #b17 tt 0
