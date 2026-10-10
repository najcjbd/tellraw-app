# [R1] !0：0和3都有 -> 命中（已测） | 期望匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
replaceitem entity @s slot.hotbar 3 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=3}] run scoreboard players set #r1s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=3}] run scoreboard players set #r1s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=!0}] run scoreboard players set #r1 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=!0}] run scoreboard players set #r1 tt 0
