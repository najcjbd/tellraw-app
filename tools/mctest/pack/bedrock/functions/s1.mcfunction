# [S1] ★分数未设置 + =!10：应不命中（验证 =! 含'存在'） | 期望不匹配
scoreboard objectives add tt dummy
scoreboard players reset @s tt
execute if entity @s[scores={tt=!10}] run scoreboard players set #s1 tt 1
execute unless entity @s[scores={tt=!10}] run scoreboard players set #s1 tt 0
