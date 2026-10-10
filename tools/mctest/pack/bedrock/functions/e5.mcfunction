# [E5] 快捷栏任意格 | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 5 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #e5s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=5}] run scoreboard players set #e5s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar}] run scoreboard players set #e5 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar}] run scoreboard players set #e5 tt 0
