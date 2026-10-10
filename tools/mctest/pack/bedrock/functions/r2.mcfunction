# [R2] !0：只0有 -> 不命中（已测） | 期望不匹配
scoreboard objectives add tt dummy
clear @s
replaceitem entity @s slot.hotbar 0 diamond 1
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #r2s tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=0}] run scoreboard players set #r2s tt 0
execute if entity @s[hasitem={item=diamond,location=slot.hotbar,slot=!0}] run scoreboard players set #r2 tt 1
execute unless entity @s[hasitem={item=diamond,location=slot.hotbar,slot=!0}] run scoreboard players set #r2 tt 0
