# [S4] ★多项反选(tt=10,tt2未设置)：读法①不命中 / 读法②命中 | 只看实测值
scoreboard objectives add tt dummy
scoreboard players set @s tt 10
execute if entity @s[scores={tt=10}] run scoreboard players set #s4s tt 1
execute unless entity @s[scores={tt=10}] run scoreboard players set #s4s tt 0
execute if entity @s[scores={tt=!10,tt2=!5}] run scoreboard players set #s4 tt 1
execute unless entity @s[scores={tt=!10,tt2=!5}] run scoreboard players set #s4 tt 0
