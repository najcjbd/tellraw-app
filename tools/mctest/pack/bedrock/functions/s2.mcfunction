# [S2] 分数=10 -> 不命中 | 期望不匹配
scoreboard objectives add tt dummy
scoreboard players set @s tt 10
execute if entity @s[scores={tt=10}] run scoreboard players set #s2s tt 1
execute unless entity @s[scores={tt=10}] run scoreboard players set #s2s tt 0
execute if entity @s[scores={tt=!10}] run scoreboard players set #s2 tt 1
execute unless entity @s[scores={tt=!10}] run scoreboard players set #s2 tt 0
