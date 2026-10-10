# [E4] 快捷栏5 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #e4s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #e4s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #e4 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #e4 tt 0
